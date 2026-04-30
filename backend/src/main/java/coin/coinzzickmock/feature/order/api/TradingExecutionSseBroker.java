package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ConcurrentMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Semaphore> memberSubscriberLimits = new ConcurrentHashMap<>();
    private final Executor sseEventExecutor;
    private final int maxSubscribersPerMember;
    private final Semaphore totalSubscriberLimit;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public TradingExecutionSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            @Value("${coin.trading.sse.max-subscribers-per-member:10}") int maxSubscribersPerMember,
            @Value("${coin.trading.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.maxSubscribersPerMember = maxSubscribersPerMember;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
        this.sseTelemetry = sseTelemetry;
    }

    TradingExecutionSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerMember,
            int maxSubscribersTotal
    ) {
        this(sseEventExecutor, maxSubscribersPerMember, maxSubscribersTotal, SseTelemetry.noop());
    }

    public SseSubscriptionPermit reserve(Long memberId) {
        if (!totalSubscriberLimit.tryAcquire()) {
            recordConnectionRejected("total_limit");
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }

        Semaphore memberLimit = memberSubscriberLimits.computeIfAbsent(
                memberId,
                key -> new Semaphore(maxSubscribersPerMember, true)
        );
        if (!memberLimit.tryAcquire()) {
            totalSubscriberLimit.release();
            recordConnectionRejected("member_limit");
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS, "거래 이벤트 스트림 연결이 너무 많습니다.");
        }

        return new SseSubscriptionPermit(memberId, memberLimit);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        bindLifecycle(permit, emitter);
        emitters.computeIfAbsent(permit.memberId(), key -> new CopyOnWriteArrayList<>()).add(emitter);
        permit.markRegistered();
        recordConnectionOpened();
    }

    public void unregister(Long memberId, SseEmitter emitter) {
        unregister(memberId, emitter, "client_complete");
    }

    private void unregister(Long memberId, SseEmitter emitter, String reason) {
        CopyOnWriteArrayList<SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters == null) {
            return;
        }

        boolean removed = memberEmitters.remove(emitter);
        if (memberEmitters.isEmpty()) {
            emitters.remove(memberId, memberEmitters);
        }
        if (removed) {
            release(memberId);
            recordConnectionClosed(reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        if (permit.markReleased()) {
            release(permit.memberId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradingExecution(TradingExecutionEvent event) {
        CopyOnWriteArrayList<SseEmitter> memberEmitters = emitters.get(event.memberId());
        if (memberEmitters == null || memberEmitters.isEmpty()) {
            return;
        }

        TradingExecutionEventResponse response = TradingExecutionEventResponse.from(event);
        try {
            sseEventExecutor.execute(() -> sendToSubscribers(event.memberId(), memberEmitters, response));
        } catch (RejectedExecutionException exception) {
            log.debug("Trading SSE executor rejected fan-out. memberId={}", event.memberId(), exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(permit.memberId(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(permit.memberId(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Trading SSE emitter reported an error; closing subscription. memberId={}", permit.memberId(), error);
            unregister(permit.memberId(), emitter, "error");
        });
    }

    private void send(Long memberId, SseEmitter emitter, TradingExecutionEventResponse response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException exception) {
            log.debug("Trading SSE send failed; closing subscription. memberId={}", memberId, exception);
            recordSend("failure", startedAt);
            unregister(memberId, emitter, "send_failure");
        }
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
            CopyOnWriteArrayList<SseEmitter> memberEmitters,
            TradingExecutionEventResponse response
    ) {
        memberEmitters.forEach(emitter -> send(memberId, emitter, response));
    }

    private void release(Long memberId) {
        totalSubscriberLimit.release();
        Semaphore memberLimit = memberSubscriberLimits.get(memberId);
        if (memberLimit != null) {
            memberLimit.release();
            cleanupMemberLimit(memberId, memberLimit);
        }
    }

    private void cleanupMemberLimit(Long memberId, Semaphore memberLimit) {
        if (memberLimit.availablePermits() == maxSubscribersPerMember && !emitters.containsKey(memberId)) {
            memberSubscriberLimits.remove(memberId, memberLimit);
        }
    }

    public static final class SseSubscriptionPermit {
        private final Long memberId;
        private final Semaphore memberLimit;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private SseSubscriptionPermit(Long memberId, Semaphore memberLimit) {
            this.memberId = memberId;
            this.memberLimit = memberLimit;
        }

        public Long memberId() {
            return memberId;
        }

        private void markRegistered() {
            registered.set(true);
        }

        private boolean markReleased() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
