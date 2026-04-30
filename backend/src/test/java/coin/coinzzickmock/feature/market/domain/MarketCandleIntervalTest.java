package coin.coinzzickmock.feature.market.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarketCandleIntervalTest {
    @Test
    void distinguishesMinuteAndMonthCaseSensitively() {
        assertThat(MarketCandleInterval.from("1m")).isEqualTo(MarketCandleInterval.ONE_MINUTE);
        assertThat(MarketCandleInterval.from("1M")).isEqualTo(MarketCandleInterval.ONE_MONTH);
    }
}
