package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompletedHourlyCandleBuilderTest {
    private final CompletedHourlyCandleBuilder builder = new CompletedHourlyCandleBuilder();

    @Test
    void buildsHourlyCandleOnlyWhenAllSixtyMinuteCandlesAreContiguous() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minutes(hourOpenTime, -1)
        );

        assertThat(result).hasValueSatisfying(candle -> {
            assertThat(candle.openTime()).isEqualTo(hourOpenTime);
            assertThat(candle.closeTime()).isEqualTo(hourOpenTime.plusSeconds(3600));
            assertThat(candle.sourceMinuteOpenTime()).isEqualTo(hourOpenTime);
            assertThat(candle.sourceMinuteCloseTime()).isEqualTo(hourOpenTime.plusSeconds(3600));
        });
    }

    @Test
    void rejectsHourlyCandleWhenMinuteCountIsShort() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minutes(hourOpenTime, 59)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsHourlyCandleWhenMinuteCoverageHasGap() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>(minutes(hourOpenTime, 30));
        minuteCandles.add(minute(hourOpenTime.plusSeconds(3600)));

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minuteCandles
        );

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsHourlyCandleWhenMinuteCoverageContainsNullElement() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>(minutes(hourOpenTime, -1));
        minuteCandles.set(30, null);

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minuteCandles
        );

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsHourlyCandleWhenMinuteCoverageContainsNullOpenTime() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>(minutes(hourOpenTime, -1));
        minuteCandles.set(30, minute(null, hourOpenTime.plusSeconds(31 * 60L)));

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minuteCandles
        );

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsHourlyCandleWhenMinuteCoverageContainsNullCloseTime() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>(minutes(hourOpenTime, -1));
        minuteCandles.set(30, minute(hourOpenTime.plusSeconds(30 * 60L), null));

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                minuteCandles
        );

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsHourlyCandleWhenHourlyBoundaryIsNull() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        assertAll(
                () -> assertThat(builder.build(1L, null, hourOpenTime.plusSeconds(3600), minutes(hourOpenTime, -1)))
                        .isEmpty(),
                () -> assertThat(builder.build(1L, hourOpenTime, null, minutes(hourOpenTime, -1))).isEmpty()
        );
    }

    @Test
    void rejectsHourlyCandleWhenMinuteListIsNullOrEmpty() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        assertAll(
                () -> assertThat(builder.build(1L, hourOpenTime, hourOpenTime.plusSeconds(3600), null)).isEmpty(),
                () -> assertThat(builder.build(1L, hourOpenTime, hourOpenTime.plusSeconds(3600), List.of())).isEmpty()
        );
    }

    @Test
    void acceptsHourlyBoundaryAndMinuteCoverageWithinSameSecond() {
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00.123Z");
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            Instant openTime = hourOpenTime.plusSeconds(minute * 60L);
            minuteCandles.add(minute(openTime, openTime.plusSeconds(60)));
        }

        Optional<HourlyMarketCandle> result = builder.build(
                1L,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600).plusNanos(456_000_000),
                minuteCandles
        );

        assertThat(result).hasValueSatisfying(candle -> {
            assertThat(candle.openTime()).isEqualTo(hourOpenTime);
            assertThat(candle.closeTime()).isEqualTo(hourOpenTime.plusSeconds(3600).plusNanos(456_000_000));
            assertThat(candle.sourceMinuteOpenTime()).isEqualTo(hourOpenTime);
            assertThat(candle.sourceMinuteCloseTime()).isEqualTo(hourOpenTime.plusSeconds(3600));
        });
    }

    private static List<MarketHistoryCandle> minutes(Instant hourOpenTime, int missingMinuteIndex) {
        List<MarketHistoryCandle> minuteCandles = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            if (minute == missingMinuteIndex) {
                continue;
            }
            minuteCandles.add(minute(hourOpenTime.plusSeconds(minute * 60L)));
        }
        return minuteCandles;
    }

    private static MarketHistoryCandle minute(Instant openTime) {
        int offset = (int) ((openTime.getEpochSecond() / 60) % 60);
        return minute(openTime, openTime.plusSeconds(60), offset);
    }

    private static MarketHistoryCandle minute(Instant openTime, Instant closeTime) {
        int offset = openTime == null ? 0 : (int) ((openTime.getEpochSecond() / 60) % 60);
        return minute(openTime, closeTime, offset);
    }

    private static MarketHistoryCandle minute(Instant openTime, Instant closeTime, int offset) {
        return new MarketHistoryCandle(
                1L,
                openTime,
                closeTime,
                100 + offset,
                101 + offset,
                99 + offset,
                100.5 + offset,
                10,
                1005
        );
    }
}
