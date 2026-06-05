package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketLatestCandleWindowPolicyTest {
    private final MarketLatestCandleWindowPolicy policy = new MarketLatestCandleWindowPolicy();

    @Test
    void bypassesCursorRequestsAndUnsupportedLimits() {
        assertFalse(policy.isEligible(Instant.parse("2026-04-21T00:00:00Z"), 120));
        assertFalse(policy.isEligible(null, 121));
    }

    @Test
    void allowsInitialFrontendAndK6Limits() {
        for (int limit : List.of(60, 104, 120, 168, 180)) {
            assertTrue(policy.isEligible(null, limit));
        }
    }

    @Test
    void keyUsesOnlyServerBoundaryIntervalSymbolAndLimit() {
        RestVisibleCandleBoundary first = new RestVisibleCandleBoundary(
                1L,
                MarketCandleInterval.ONE_MINUTE,
                Instant.parse("2026-04-21T00:00:00Z")
        );
        RestVisibleCandleBoundary second = new RestVisibleCandleBoundary(
                1L,
                MarketCandleInterval.ONE_MINUTE,
                Instant.parse("2026-04-21T00:01:00Z")
        );

        MarketLatestCandleWindowKey firstKey = policy.key("BTCUSDT", first, 120);
        MarketLatestCandleWindowKey secondKey = policy.key("BTCUSDT", second, 120);

        assertEquals("market:latest-candles:BTCUSDT:1m:limit120:closedUntil:1776729600000", firstKey.cacheKey());
        assertNotEquals(firstKey.cacheKey(), secondKey.cacheKey());
    }
}
