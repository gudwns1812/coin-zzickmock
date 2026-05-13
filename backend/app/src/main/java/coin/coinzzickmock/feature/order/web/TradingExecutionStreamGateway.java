package coin.coinzzickmock.feature.order.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface TradingExecutionStreamGateway {
    SubscriptionPermit reserve(Long memberId, String clientKey);

    void register(SubscriptionPermit permit, SseEmitter emitter);

    void release(SubscriptionPermit permit);

    interface SubscriptionPermit {
    }
}
