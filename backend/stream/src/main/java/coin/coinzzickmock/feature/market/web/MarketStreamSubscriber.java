package coin.coinzzickmock.feature.market.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

record MarketStreamSubscriber(MarketStreamSessionKey sessionKey, SseEmitter emitter) {
}
