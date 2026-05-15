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
    void evictsOldAcceptedTradeIdsAfterBoundedRetention() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore(2);

        assertThat(store.acceptTrade(trade("111", "27000", "2026-04-30T04:00:00Z"))).isTrue();
        assertThat(store.acceptTrade(trade("222", "27001", "2026-04-30T04:00:01Z"))).isTrue();
        assertThat(store.acceptTrade(trade("333", "27002", "2026-04-30T04:00:02Z"))).isTrue();

        assertThat(store.acceptTrade(trade("111", "27003", "2026-04-30T04:00:03Z"))).isTrue();
        assertThat(store.latestTrade("BTCUSDT")).get()
                .extracting(state -> state.price())
                .isEqualTo(new BigDecimal("27003"));
    }

    @Test
    void rejectedStaleTradeIdsDoNotEvictAcceptedTradeIds() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore(2);

        assertThat(store.acceptTrade(trade("111", "27000", "2026-04-30T04:00:02Z"))).isTrue();
        assertThat(store.acceptTrade(trade("222", "27001", "2026-04-30T04:00:03Z"))).isTrue();
        assertThat(store.acceptTrade(trade("333", "26999", "2026-04-30T04:00:00Z"))).isFalse();
        assertThat(store.acceptTrade(trade("444", "26998", "2026-04-30T04:00:01Z"))).isFalse();

        assertThat(store.acceptTrade(trade("111", "27002", "2026-04-30T04:00:04Z"))).isFalse();
        assertThat(store.latestTrade("BTCUSDT")).get()
                .extracting(state -> state.price())
                .isEqualTo(new BigDecimal("27001"));
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
    void rejectsOlderTradeWithDifferentTradeIdAndReturnsAcceptedMovement() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();

        RealtimeMarketDataStore.AcceptedTradeUpdate first = store.acceptTradeUpdate(
                trade("111", "27000", "2026-04-30T04:00:01Z")
        );
        RealtimeMarketDataStore.AcceptedTradeUpdate older = store.acceptTradeUpdate(
                trade("112", "26000", "2026-04-30T04:00:00Z")
        );
        RealtimeMarketDataStore.AcceptedTradeUpdate next = store.acceptTradeUpdate(
                trade("113", "26900", "2026-04-30T04:00:02Z")
        );

        assertThat(first.accepted()).isTrue();
        assertThat(first.movement()).isEmpty();
        assertThat(older.accepted()).isFalse();
        assertThat(older.movement()).isEmpty();
        assertThat(next.accepted()).isTrue();
        assertThat(next.movement()).get()
                .extracting(MarketTradePriceMovedEvent::previousLastPrice, MarketTradePriceMovedEvent::currentLastPrice,
                        MarketTradePriceMovedEvent::direction)
                .containsExactly(27000.0, 26900.0, MarketPriceMovementDirection.DOWN);
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
