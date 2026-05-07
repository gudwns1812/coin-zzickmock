package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketRealtimeSseBroker {
    private static final String STREAM = "market";
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final SseSubscriptionRegistry<String> subscriptions;
    private final Executor sseEventExecutor;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketRealtimeSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerSymbol,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.subscriptions = new SseSubscriptionRegistry<>(maxSubscribersPerSymbol, maxSubscribersTotal);
        this.sseTelemetry = sseTelemetry;
    }

    MarketRealtimeSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerSymbol,
            int maxSubscribersTotal
    ) {
        this(sseEventExecutor, maxSubscribersPerSymbol, maxSubscribersTotal, NoopSseTelemetry.INSTANCE);
    }

    public SseSubscriptionPermit reserve(String symbol) {
        return reserve(subscriptions.reserve(symbol));
    }

    public SseSubscriptionPermit reserve(String symbol, String clientKey) {
        return reserve(subscriptions.reserve(symbol, clientKey));
    }

    private SseSubscriptionPermit reserve(SseSubscriptionRegistry.Reservation<String> reservation) {
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
        logLifecycle(permit.symbol(), "register", "accepted");
        completeReplacedEmitter(permit.symbol(), registration.replacedEmitter());
    }

    public void unregister(String symbol, SseEmitter emitter) {
        unregister(symbol, emitter, "client_complete");
    }

    private void unregister(String symbol, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(symbol, emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, "unregister", reason);
        }
    }

    private void unregister(String symbol, String clientKey, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(symbol, clientKey, emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, "unregister", reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        if (subscriptions.release(permit.delegate)) {
            logLifecycle(permit.symbol(), "release", "before_register");
        }
    }

    @EventListener
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        String symbol = event.result().symbol();
        List<SseEmitter> symbolEmitters = subscriptions.subscribers(symbol);
        if (symbolEmitters.isEmpty()) {
            return;
        }

        MarketSummaryResponse response = MarketSummaryResponse.from(event.result());
        try {
            sseEventExecutor.execute(() -> sendToSubscribers(symbol, symbolEmitters, response));
        } catch (RejectedExecutionException exception) {
            log.debug("Market SSE executor rejected fan-out. symbol={}", symbol, exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(permit.symbol(), permit.clientKey(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(permit.symbol(), permit.clientKey(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market SSE emitter reported an error; closing subscription. symbol={}", permit.symbol(), error);
            unregister(permit.symbol(), permit.clientKey(), emitter, "error");
        });
    }

    private void send(String symbol, SseEmitter emitter, MarketSummaryResponse response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException exception) {
            log.debug("Market SSE send failed; closing subscription. symbol={}", symbol, exception);
            recordSend("failure", startedAt);
            unregister(symbol, emitter, "send_failure");
        }
    }

    private void sendToSubscribers(String symbol, List<SseEmitter> symbolEmitters,
                                   MarketSummaryResponse response) {
        symbolEmitters.forEach(emitter -> send(symbol, emitter, response));
    }


    private void reject(ReservationRejection rejection) {
        if (rejection == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            return;
        }
        recordConnectionRejected("symbol_limit");
    }

    private void completeReplacedEmitter(String symbol, SseEmitter replacedEmitter) {
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
            log.warn("Failed to record market SSE connection open telemetry", exception);
        }
    }

    private void recordConnectionClosed(String reason) {
        try {
            sseTelemetry.connectionClosed(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE connection close telemetry. reason={}", reason, exception);
        }
    }

    private void recordConnectionRejected(String reason) {
        try {
            sseTelemetry.connectionRejected(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE connection rejection telemetry. reason={}", reason, exception);
        }
    }

    private void recordSend(String result, long startedAt) {
        try {
            sseTelemetry.sendRecorded(STREAM, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE send telemetry. result={}", result, exception);
        }
    }

    private void recordExecutorRejected() {
        try {
            sseTelemetry.executorRejected(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE executor rejection telemetry", exception);
        }
    }

    boolean hasSubscriberLimit(String symbol) {
        return subscriptions.hasSubscriberLimit(symbol);
    }

    public static final class SseSubscriptionPermit {
        private final SseSubscriptionRegistry.Permit<String> delegate;

        private SseSubscriptionPermit(SseSubscriptionRegistry.Permit<String> delegate) {
            this.delegate = delegate;
        }

        public String symbol() {
            return delegate.key();
        }

        public String clientKey() {
            return delegate.clientKey();
        }
    }
}
