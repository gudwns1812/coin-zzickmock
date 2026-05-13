package coin.coinzzickmock.feature.order.web.config;

import coin.coinzzickmock.feature.order.web.TradingExecutionStreamGateway;
import coin.coinzzickmock.feature.order.web.TradingExecutionSseBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
class StreamTradingExecutionGatewayAdapter implements TradingExecutionStreamGateway {
    private final TradingExecutionSseBroker tradingExecutionSseBroker;

    @Override
    public SubscriptionPermit reserve(Long memberId, String clientKey) {
        return new BrokerSubscriptionPermit(tradingExecutionSseBroker.reserve(memberId, clientKey));
    }

    @Override
    public void register(SubscriptionPermit permit, SseEmitter emitter) {
        tradingExecutionSseBroker.register(unwrap(permit), emitter);
    }

    @Override
    public void release(SubscriptionPermit permit) {
        tradingExecutionSseBroker.release(unwrap(permit));
    }

    private TradingExecutionSseBroker.SseSubscriptionPermit unwrap(SubscriptionPermit permit) {
        if (permit instanceof BrokerSubscriptionPermit brokerPermit) {
            return brokerPermit.delegate;
        }
        throw new IllegalArgumentException("Unknown trading execution stream permit type: " + permit.getClass().getName());
    }

    private record BrokerSubscriptionPermit(
            TradingExecutionSseBroker.SseSubscriptionPermit delegate
    ) implements SubscriptionPermit {
    }
}
