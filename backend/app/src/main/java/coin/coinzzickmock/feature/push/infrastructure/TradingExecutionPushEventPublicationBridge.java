package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.push.application.publisher.PushEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class TradingExecutionPushEventPublicationBridge {
    private final PushEventPublisher pushEventPublisher;
    private final PushEventEnvelopeFactory pushEventEnvelopeFactory;

    @EventListener
    void onTradingExecution(TradingExecutionEvent event) {
        try {
            pushEventPublisher.publish(pushEventEnvelopeFactory.tradingExecution(event));
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to publish trading execution push event. memberFingerprint={} type={} symbol={}",
                    memberFingerprint(event.memberId()),
                    event.type(),
                    event.symbol(),
                    exception
            );
        }
    }

    private String memberFingerprint(Long memberId) {
        return Integer.toHexString(Long.hashCode(memberId));
    }
}
