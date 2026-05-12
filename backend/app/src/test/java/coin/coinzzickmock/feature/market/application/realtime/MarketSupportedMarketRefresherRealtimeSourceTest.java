package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;

class MarketSupportedMarketRefresherRealtimeSourceTest {
    @Test
    void usesRealtimeStoreWithoutRestWhenRealtimeTickerIsAvailable() {
        CountingMarketDataGateway marketDataGateway = new CountingMarketDataGateway();
        MarketSnapshotStore snapshotStore = new MarketSnapshotStore(new ConcurrentMapCacheManager(
                CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE,
                CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE
        ));
        snapshotStore.putSupportedMarkets(List.of(new MarketSummaryResult(
                "BTCUSDT",
                "Bitcoin",
                1,
                2,
                3,
                4,
                5,
                6
        )));
        RealtimeMarketDataStore realtimeStore = new RealtimeMarketDataStore();
        realtimeStore.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                new BigDecimal("27000"),
                new BigDecimal("27001"),
                new BigDecimal("27002"),
                new BigDecimal("0.0001"),
                Instant.parse("2026-04-30T08:00:00Z"),
                Instant.parse("2026-04-30T04:00:00Z"),
                Instant.parse("2026-04-30T04:00:00Z")
        ));
        realtimeStore.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-1",
                new BigDecimal("27010"),
                new BigDecimal("0.01"),
                "buy",
                Instant.parse("2026-04-30T04:00:01Z"),
                Instant.parse("2026-04-30T04:00:01Z")
        ));
        MarketSupportedMarketRefresher refresher = new MarketSupportedMarketRefresher(
                marketDataGateway,
                snapshotStore,
                event -> {
                },
                symbol -> FundingSchedule.defaultUsdtPerpetual(),
                new RealtimeMarketSummaryProjector(realtimeStore, symbol -> FundingSchedule.defaultUsdtPerpetual())
        );

        List<MarketSummaryResult> refreshed = refresher.refreshSupportedMarkets();

        assertThat(refreshed).hasSize(1);
        assertThat(refreshed.get(0).lastPrice()).isEqualTo(27010);
        assertThat(refreshed.get(0).markPrice()).isEqualTo(27001);
        assertThat(marketDataGateway.loadSupportedMarketsCalls()).isZero();
        assertThat(marketDataGateway.loadMarketCalls()).isZero();
    }

    private static final class CountingMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private int loadSupportedMarketsCalls;
        private int loadMarketCalls;

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            loadSupportedMarketsCalls++;
            return List.of(new MarketSnapshot("BTCUSDT", "Bitcoin", 1, 2, 3, 4, 5, 6));
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            loadMarketCalls++;
            return new MarketSnapshot(symbol, symbol, 1, 2, 3, 4, 5, 6);
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return List.of();
        }

        @Override
        public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
                String symbol,
                MarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive,
                int limit
        ) {
            return List.of();
        }

        int loadSupportedMarketsCalls() {
            return loadSupportedMarketsCalls;
        }

        int loadMarketCalls() {
            return loadMarketCalls;
        }
    }

    private record FakeProviders(MarketDataGateway marketDataGateway) implements Providers {
        @Override
        public AuthProvider auth() {
            return null;
        }

        @Override
        public ConnectorProvider connector() {
            return null;
        }

        @Override
        public TelemetryProvider telemetry() {
            return null;
        }

        @Override
        public FeatureFlagProvider featureFlags() {
            return null;
        }
    }
}
