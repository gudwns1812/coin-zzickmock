package coin.coinzzickmock.feature.order.web.config;

import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.web.TradingExecutionSseBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class TradingExecutionStreamEventBridge {
    private final TradingExecutionSseBroker tradingExecutionSseBroker;

    @EventListener
    public void onTradingExecution(TradingExecutionEvent event) {
        try {
            tradingExecutionSseBroker.onTradingExecution(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to fan out trading execution SSE update. memberFingerprint={} type={} symbol={}",
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
