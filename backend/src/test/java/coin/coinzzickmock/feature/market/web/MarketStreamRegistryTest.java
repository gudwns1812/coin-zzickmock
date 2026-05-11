package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketStreamRegistryTest {

    @Test
    void staleReplacedEmitterCannotReleaseCurrentSession() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        MarketStreamSessionKey key = new MarketStreamSessionKey(1L, "tab:demo");
        SseEmitter firstEmitter = new SseEmitter();
        SseEmitter secondEmitter = new SseEmitter();
        CandleSubscription btcCandle = new CandleSubscription("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
        CandleSubscription ethCandle = new CandleSubscription("ETHUSDT", MarketCandleInterval.ONE_MINUTE);

        registry.registerSession(key, firstEmitter, "BTCUSDT", Set.of(), btcCandle);
        registry.registerSession(key, secondEmitter, "ETHUSDT", Set.of(), ethCandle);

        assertEquals(0, registry.candleSubscriberCount(btcCandle));
        assertEquals(1, registry.candleSubscriberCount(ethCandle));

        assertFalse(registry.releaseSession(key, firstEmitter, "client_complete"));
        assertEquals(1, registry.candleSubscriberCount(ethCandle));

        assertTrue(registry.releaseSession(key, secondEmitter, "client_complete"));
        assertEquals(0, registry.candleSubscriberCount(ethCandle));
    }
}
