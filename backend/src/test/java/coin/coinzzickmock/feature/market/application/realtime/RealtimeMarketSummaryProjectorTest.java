package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealtimeMarketSummaryProjectorTest {
    @Test
    void projectsTickerAndTradeIntoMarketSummary() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketSummaryProjector projector = new RealtimeMarketSummaryProjector(
                store,
                symbol -> coin.coinzzickmock.feature.market.domain.FundingSchedule.defaultUsdtPerpetual()
        );
        store.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                new BigDecimal("27000"),
                new BigDecimal("27001"),
                new BigDecimal("27002"),
                new BigDecimal("0.0001"),
                Instant.parse("2026-04-30T08:00:00Z"),
                Instant.parse("2026-04-30T04:00:00Z"),
                Instant.parse("2026-04-30T04:00:00Z")
        ));
        store.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-1",
                new BigDecimal("27010"),
                new BigDecimal("0.01"),
                "buy",
                Instant.parse("2026-04-30T04:00:01Z"),
                Instant.parse("2026-04-30T04:00:01Z")
        ));
        MarketSummaryResult previous = new MarketSummaryResult("BTCUSDT", "Bitcoin", 1, 2, 3, 4, 5, 6);

        MarketSummaryResult projected = projector.project("BTCUSDT", previous).orElseThrow();

        assertThat(projected.displayName()).isEqualTo("Bitcoin");
        assertThat(projected.lastPrice()).isEqualTo(27010);
        assertThat(projected.markPrice()).isEqualTo(27001);
        assertThat(projected.indexPrice()).isEqualTo(27002);
        assertThat(projected.fundingRate()).isEqualTo(0.0001);
        assertThat(projected.change24h()).isEqualTo(5);
        assertThat(projected.turnover24hUsdt()).isEqualTo(6);
    }

    @Test
    void returnsEmptyWhenTickerIsMissing() {
        RealtimeMarketSummaryProjector projector = new RealtimeMarketSummaryProjector(
                new RealtimeMarketDataStore(),
                symbol -> coin.coinzzickmock.feature.market.domain.FundingSchedule.defaultUsdtPerpetual()
        );

        assertThat(projector.project("BTCUSDT", null)).isEmpty();
    }
}
