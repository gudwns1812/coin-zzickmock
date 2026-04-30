package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealtimeMarketDataStoreTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-04-30T04:00:00Z");

    @Test
    void ignoresDuplicateTradeIds() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();

        assertThat(store.acceptTrade(trade("111", "27000", "2026-04-30T04:00:00Z"))).isTrue();
        assertThat(store.acceptTrade(trade("111", "28000", "2026-04-30T04:00:01Z"))).isFalse();

        assertThat(store.latestTrade("BTCUSDT")).get()
                .extracting(state -> state.price())
                .isEqualTo(new BigDecimal("27000"));
    }

    @Test
    void ordersTradesWithoutTradeIdBySourceTimeAndReceiveSequence() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();

        assertThat(store.acceptTrade(trade(null, "27000", "2026-04-30T04:00:01Z"))).isTrue();
        assertThat(store.acceptTrade(trade(null, "26000", "2026-04-30T04:00:00Z"))).isFalse();
        assertThat(store.acceptTrade(trade(null, "27100", "2026-04-30T04:00:01Z"))).isTrue();

        assertThat(store.latestTrade("BTCUSDT")).get()
                .extracting(state -> state.price())
                .isEqualTo(new BigDecimal("27100"));
    }

    @Test
    void ordersTickerUpdatesBySourceTimeAndReceiveSequence() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();

        assertThat(store.acceptTicker(ticker("27000", "2026-04-30T04:00:01Z"))).isTrue();
        assertThat(store.acceptTicker(ticker("26000", "2026-04-30T04:00:00Z"))).isFalse();
        assertThat(store.acceptTicker(ticker("27100", "2026-04-30T04:00:01Z"))).isTrue();

        assertThat(store.latestTicker("BTCUSDT")).get()
                .extracting(state -> state.lastPrice())
                .isEqualTo(new BigDecimal("27100"));
    }

    @Test
    void upsertsCandlesBySymbolIntervalAndOpenTime() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        Instant openTime = Instant.parse("2026-04-30T04:00:00Z");

        assertThat(store.acceptCandle(candle(openTime, "27000", "2026-04-30T04:00:01Z"))).isTrue();
        assertThat(store.acceptCandle(candle(openTime, "27050", "2026-04-30T04:00:02Z"))).isTrue();

        assertThat(store.candle("BTCUSDT", MarketCandleInterval.ONE_MINUTE, openTime)).get()
                .extracting(state -> state.closePrice())
                .isEqualTo(new BigDecimal("27050"));
    }

    @Test
    void recordsRestFallbacksWithExplicitLabels() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();

        store.recordRestBootstrap("BTCUSDT", RECEIVED_AT.minusSeconds(1), RECEIVED_AT, "startup seed");
        store.recordRestRecovery("BTCUSDT", RECEIVED_AT.minusMillis(500), RECEIVED_AT, "ws reconnect gap repair");

        assertThat(store.fallbackSource("BTCUSDT", MarketRealtimeSourceType.REST_BOOTSTRAP)).get()
                .extracting(MarketRealtimeSourceSnapshot::fallbackReason)
                .isEqualTo("startup seed");
        assertThat(store.fallbackSource("BTCUSDT", MarketRealtimeSourceType.REST_RECOVERY)).get()
                .extracting(MarketRealtimeSourceSnapshot::health)
                .isEqualTo(MarketRealtimeHealth.RECOVERING);
    }

    private RealtimeMarketTradeTick trade(String tradeId, String price, String sourceEventTime) {
        return new RealtimeMarketTradeTick(
                "BTCUSDT",
                tradeId,
                new BigDecimal(price),
                new BigDecimal("0.001"),
                "buy",
                Instant.parse(sourceEventTime),
                RECEIVED_AT
        );
    }

    private RealtimeMarketTickerUpdate ticker(String lastPrice, String sourceEventTime) {
        return new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                new BigDecimal(lastPrice),
                new BigDecimal("27001"),
                new BigDecimal("27002"),
                new BigDecimal("0.0001"),
                Instant.parse("2026-04-30T08:00:00Z"),
                Instant.parse(sourceEventTime),
                RECEIVED_AT
        );
    }

    private RealtimeMarketCandleUpdate candle(Instant openTime, String closePrice, String sourceEventTime) {
        return new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                openTime,
                new BigDecimal("27000"),
                new BigDecimal("27100"),
                new BigDecimal("26900"),
                new BigDecimal(closePrice),
                new BigDecimal("1.0"),
                new BigDecimal("27000"),
                new BigDecimal("27000"),
                Instant.parse(sourceEventTime),
                RECEIVED_AT
        );
    }
}
