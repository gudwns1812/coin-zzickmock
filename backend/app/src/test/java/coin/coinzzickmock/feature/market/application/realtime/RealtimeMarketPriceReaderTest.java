package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealtimeMarketPriceReaderTest {
    @Test
    void returnsFreshTradeAndTickerPrices() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketPriceReader reader = new RealtimeMarketPriceReader(store);
        Instant now = Instant.now();
        store.acceptTrade(trade(now, "27010"));
        store.acceptTicker(ticker(now, "27000", "27001"));

        MarketSnapshot market = reader.requireFreshMarket("BTCUSDT");

        assertThat(market.lastPrice()).isEqualTo(27010.0);
        assertThat(market.markPrice()).isEqualTo(27001.0);
        assertThat(reader.freshMarkPrice("BTCUSDT")).contains(27001.0);
    }

    @Test
    void rejectsWhenTradeOrTickerIsStale() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketPriceReader reader = new RealtimeMarketPriceReader(store);
        Instant stale = Instant.now().minusSeconds(60);
        store.acceptTrade(trade(stale, "27010"));
        store.acceptTicker(ticker(Instant.now(), "27000", "27001"));

        assertThat(reader.freshMarket("BTCUSDT")).isEmpty();
        assertThatThrownBy(() -> reader.requireFreshMarket("BTCUSDT"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).errorCode())
                .isEqualTo(ErrorCode.MARKET_NOT_FOUND);
    }

    @Test
    void readMarkPriceCanUseFreshTickerWithoutTrade() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketPriceReader reader = new RealtimeMarketPriceReader(store);
        store.acceptTicker(ticker(Instant.now(), "27000", "27001"));

        assertThat(reader.freshMarkPrice("BTCUSDT")).contains(27001.0);
        assertThat(reader.freshMarket("BTCUSDT")).isEmpty();
    }

    private static RealtimeMarketTradeTick trade(Instant at, String price) {
        return new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-" + price + "-" + at.toEpochMilli(),
                new BigDecimal(price),
                BigDecimal.ONE,
                "buy",
                at,
                at
        );
    }

    private static RealtimeMarketTickerUpdate ticker(Instant at, String lastPrice, String markPrice) {
        return new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                new BigDecimal(lastPrice),
                new BigDecimal(markPrice),
                new BigDecimal(markPrice),
                new BigDecimal("0.0001"),
                at.plusSeconds(3600),
                at,
                at
        );
    }
}
