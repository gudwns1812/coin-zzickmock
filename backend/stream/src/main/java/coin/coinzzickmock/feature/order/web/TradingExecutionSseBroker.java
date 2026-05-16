package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseEmitterLifecycle;
import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class TradingExecutionSseBroker {
    private static final String STREAM = "trading_execution";
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final SseSubscriptionRegistry<Long> subscriptions;
    private final SseDeliveryExecutor sseEventExecutor;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public TradingExecutionSseBroker(
            SseDeliveryExecutor sseEventExecutor,
            @Value("${coin.trading.sse.max-subscribers-per-member:10}") int maxSubscribersPerMember,
            @Value("${coin.trading.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.subscriptions = new SseSubscriptionRegistry<>(maxSubscribersPerMember, maxSubscribersTotal);
        this.sseTelemetry = sseTelemetry;
    }

    TradingExecutionSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerMember,
            int maxSubscribersTotal
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), maxSubscribersPerMember, maxSubscribersTotal, NoopSseTelemetry.INSTANCE);
    }

    TradingExecutionSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerMember,
            int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), maxSubscribersPerMember, maxSubscribersTotal, sseTelemetry);
    }

    public SseSubscriptionPermit reserve(Long memberId) {
        return reserve(subscriptions.reserve(memberId));
    }

    public SseSubscriptionPermit reserve(Long memberId, String clientKey) {
        return reserve(subscriptions.reserve(memberId, clientKey));
    }

    private SseSubscriptionPermit reserve(SseSubscriptionRegistry.Reservation<Long> reservation) {
        if (reservation.accepted()) {
            return new SseSubscriptionPermit(reservation.permit());
        }
        reject(reservation.rejection());
        throw new SseSubscriptionLimitExceededException(
                reservation.rejection() == ReservationRejection.TOTAL_LIMIT ? "total_limit" : "member_limit"
        );
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        Long memberId = permit.memberId();
        try {
            bindLifecycle(permit, emitter);
            var registration = subscriptions.register(permit.delegate, emitter);
            if (!registration.registered()) {
                reject(registration.rejection());
                throw new SseSubscriptionLimitExceededException("member_limit");
            }
            recordConnectionOpened();
            logLifecycle(memberId, "register", null);
            completeReplacedEmitter(memberId, registration.replacedEmitter());
        } catch (RuntimeException exception) {
            discardRegisteredSubscription(memberId, emitter);
            release(permit);
            throw exception;
        }
    }

    public void unregister(Long memberId, SseEmitter emitter) {
        unregister(memberId, emitter, "client_complete");
    }

    private void unregister(Long memberId, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(memberId, emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(memberId, lifecycleAction(reason), reason);
        }
    }

    private void unregister(Long memberId, String clientKey, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(memberId, clientKey, emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(memberId, lifecycleAction(reason), reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        if (subscriptions.release(permit.delegate)) {
            logLifecycle(permit.memberId(), "release", "before_register");
        }
    }

    public void onTradingExecution(TradingExecutionEvent event) {
        List<SseEmitter> memberEmitters = subscriptions.subscribers(event.memberId());
        if (memberEmitters.isEmpty()) {
            return;
        }

        TradingExecutionEventResponse response = TradingExecutionEventResponse.from(event);
        try {
            sseEventExecutor.execute(() -> sendToSubscribers(event.memberId(), memberEmitters, response));
        } catch (RejectedExecutionException exception) {
            log.debug("Trading SSE executor rejected fan-out. stream=trading_execution", exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        SseEmitterLifecycle.bind(
                emitter,
                () -> unregister(permit.memberId(), permit.clientKey(), emitter, "client_complete"),
                () -> unregister(permit.memberId(), permit.clientKey(), emitter, "timeout"),
                error -> {
                    log.debug("Trading SSE emitter reported an error; closing subscription. stream=trading_execution", error);
                    unregister(permit.memberId(), permit.clientKey(), emitter, "error");
                }
        );
    }

    private void send(Long memberId, SseEmitter emitter, TradingExecutionEventResponse response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Trading SSE send failed; closing subscription. stream=trading_execution reason=send_failure", exception);
            recordSend("failure", startedAt);
            unregister(memberId, emitter, "send_failure");
        }
    }

    private void discardRegisteredSubscription(Long memberId, SseEmitter emitter) {
        subscriptions.unregister(memberId, emitter);
    }


    private void reject(ReservationRejection rejection) {
        if (rejection == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            return;
        }
        recordConnectionRejected("member_limit");
    }

    private void completeReplacedEmitter(Long memberId, SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        SseEmitterLifecycle.completeSilently(replacedEmitter);
        recordConnectionClosed("client_replaced");
        logLifecycle(memberId, "replace", "client_replaced");
    }

    private void logLifecycle(Long memberId, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=member memberFingerprint={} action={} reason={} activeKeyEmitters={} activeTotalEmitters={}",
                STREAM,
                memberFingerprint(memberId),
                action,
                reason,
                subscriptions.subscriberCount(memberId),
                subscriptions.totalSubscriberCount()
        );
    }

    private String memberFingerprint(Long memberId) {
        return Integer.toHexString(Long.hashCode(memberId));
    }

    private String lifecycleAction(String reason) {
        if ("client_complete".equals(reason)) {
            return "complete";
        }
        return reason;
    }

    private void recordConnectionOpened() {
        try {
            sseTelemetry.connectionOpened(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record trading SSE connection open telemetry", exception);
        }
    }

    private void recordConnectionClosed(String reason) {
        try {
            sseTelemetry.connectionClosed(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record trading SSE connection close telemetry. reason={}", reason, exception);
        }
    }

    private void recordConnectionRejected(String reason) {
        try {
            sseTelemetry.connectionRejected(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record trading SSE connection rejection telemetry. reason={}", reason, exception);
        }
    }

    private void recordSend(String result, long startedAt) {
        try {
            sseTelemetry.sendRecorded(STREAM, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record trading SSE send telemetry. result={}", result, exception);
        }
    }

    private void recordExecutorRejected() {
        try {
            sseTelemetry.executorRejected(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record trading SSE executor rejection telemetry", exception);
        }
    }

    private void sendToSubscribers(
            Long memberId,
            List<SseEmitter> memberEmitters,
            TradingExecutionEventResponse response
    ) {
        memberEmitters.forEach(emitter -> send(memberId, emitter, response));
    }

    public static final class SseSubscriptionPermit {
        private final SseSubscriptionRegistry.Permit<Long> delegate;

        private SseSubscriptionPermit(SseSubscriptionRegistry.Permit<Long> delegate) {
            this.delegate = delegate;
        }

        public Long memberId() {
            return delegate.key();
        }

        public String clientKey() {
            return delegate.clientKey();
        }
    }
}
