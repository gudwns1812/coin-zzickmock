package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class TradingExecutionSseBroker {
    private static final String STREAM = "trading_execution";

    private final SseSubscriptionRegistry<Long> subscriptions;
    private final Executor sseEventExecutor;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public TradingExecutionSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
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
        this(sseEventExecutor, maxSubscribersPerMember, maxSubscribersTotal, NoopSseTelemetry.INSTANCE);
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
        throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        bindLifecycle(permit, emitter);
        var registration = subscriptions.register(permit.delegate, emitter);
        if (!registration.registered()) {
            reject(registration.rejection());
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }
        recordConnectionOpened();
        completeReplacedEmitter(registration.replacedEmitter());
    }

    public void unregister(Long memberId, SseEmitter emitter) {
        unregister(memberId, emitter, "client_complete");
    }

    private void unregister(Long memberId, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(memberId, emitter)) {
            recordConnectionClosed(reason);
        }
    }

    private void unregister(Long memberId, String clientKey, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(memberId, clientKey, emitter)) {
            recordConnectionClosed(reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        subscriptions.release(permit.delegate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
        emitter.onCompletion(() -> unregister(permit.memberId(), permit.clientKey(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(permit.memberId(), permit.clientKey(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Trading SSE emitter reported an error; closing subscription. stream=trading_execution", error);
            unregister(permit.memberId(), permit.clientKey(), emitter, "error");
        });
    }

    private void send(Long memberId, SseEmitter emitter, TradingExecutionEventResponse response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException exception) {
            log.debug("Trading SSE send failed; closing subscription. stream=trading_execution reason=send_failure", exception);
            recordSend("failure", startedAt);
            unregister(memberId, emitter, "send_failure");
        }
    }


    private void reject(ReservationRejection rejection) {
        if (rejection == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            return;
        }
        recordConnectionRejected("member_limit");
    }

    private void completeReplacedEmitter(SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        try {
            replacedEmitter.complete();
        } catch (RuntimeException ignored) {
            // The replaced client may already be closed.
        }
        recordConnectionClosed("client_replaced");
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
