package coin.coinzzickmock.feature.market.candle.application.implement;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.candle.application.implement.CompletedCalendarCandleBuilder;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompletedCalendarCandleBuilderTest {
    private final CompletedCalendarCandleBuilder builder = new CompletedCalendarCandleBuilder();

    @Test
    void buildsCalendarCandleWhenHourlyCoverageIsExactAndContiguous() {
        Instant dayStart = Instant.parse("2026-04-17T00:00:00Z");
        List<HourlyMarketCandle> hourlyCandles = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyCandles.add(hourly(dayStart.plusSeconds(hour * 3600L), 100 + hour));
        }

        Optional<CompletedMarketCandle> result = builder.build(
                1L,
                MarketCandleInterval.ONE_DAY,
                dayStart,
                dayStart.plusSeconds(86_400),
                hourlyCandles
        );

        assertThat(result).isPresent();
        CompletedMarketCandle candle = result.orElseThrow();
        assertThat(candle.interval()).isEqualTo(MarketCandleInterval.ONE_DAY);
        assertThat(candle.openPrice()).isEqualTo(100.0);
        assertThat(candle.closePrice()).isEqualTo(123.5);
        assertThat(candle.highPrice()).isEqualTo(124.0);
        assertThat(candle.lowPrice()).isEqualTo(99.0);
    }

    @Test
    void skipsCalendarCandleWhenAnyHourlySourceIsMissing() {
        Instant dayStart = Instant.parse("2026-04-17T00:00:00Z");
        List<HourlyMarketCandle> hourlyCandles = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            if (hour != 12) {
                hourlyCandles.add(hourly(dayStart.plusSeconds(hour * 3600L), 100 + hour));
            }
        }

        Optional<CompletedMarketCandle> result = builder.build(
                1L,
                MarketCandleInterval.ONE_DAY,
                dayStart,
                dayStart.plusSeconds(86_400),
                hourlyCandles
        );

        assertThat(result).isEmpty();
    }

    private static HourlyMarketCandle hourly(Instant openTime, double openPrice) {
        return new HourlyMarketCandle(
                1L,
                openTime,
                openTime.plusSeconds(3600),
                openPrice,
                openPrice + 1,
                openPrice - 1,
                openPrice + 0.5,
                10,
                1000,
                openTime,
                openTime.plusSeconds(3600)
        );
    }
}
