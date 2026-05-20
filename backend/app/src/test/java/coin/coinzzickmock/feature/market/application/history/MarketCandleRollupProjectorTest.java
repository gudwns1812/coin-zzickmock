package coin.coinzzickmock.feature.market.application.history;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketCandleRollupProjectorTest {
    private final MarketCandleRollupProjector projector = new MarketCandleRollupProjector();

    @Test
    void rollsUpTwelveHourFixedUtcWindowFromCompletedHourlyRows() {
        Instant bucketStart = Instant.parse("2026-04-17T12:00:00Z");

        List<MarketCandleResult> results = projector.rollupHourlyResults(
                hourlyCandles(bucketStart, 12),
                MarketCandleInterval.TWELVE_HOURS
        );

        assertThat(results).singleElement()
                .satisfies(candle -> {
                    assertThat(candle.openTime()).isEqualTo(bucketStart);
                    assertThat(candle.closeTime()).isEqualTo(Instant.parse("2026-04-18T00:00:00Z"));
                    assertThat(candle.openPrice()).isEqualTo(100);
                    assertThat(candle.highPrice()).isEqualTo(122);
                    assertThat(candle.lowPrice()).isEqualTo(99);
                    assertThat(candle.closePrice()).isEqualTo(111.5);
                    assertThat(candle.volume()).isEqualTo(120);
                });
    }

    @Test
    void rejectsFourHourBucketWhenHourlyOpenTimesAreNotContiguousEvenIfCountMatches() {
        Instant bucketStart = Instant.parse("2026-04-17T04:00:00Z");
        List<HourlyMarketCandle> duplicatedGapCandles = List.of(
                hourly(bucketStart, 0),
                hourly(bucketStart.plusSeconds(3600), 1),
                hourly(bucketStart.plusSeconds(3600), 2),
                hourly(bucketStart.plusSeconds(3 * 3600), 3)
        );

        List<MarketCandleResult> results = projector.rollupHourlyResults(
                duplicatedGapCandles,
                MarketCandleInterval.FOUR_HOURS
        );

        assertThat(results).isEmpty();
    }

    @Test
    void rejectsMinuteBucketWhenOpenTimesAreNotContiguousEvenIfCountMatches() {
        Instant bucketStart = Instant.parse("2026-04-17T04:00:00Z");
        List<MarketHistoryCandle> duplicatedGapCandles = List.of(
                minute(bucketStart, 0),
                minute(bucketStart.plusSeconds(60), 1),
                minute(bucketStart.plusSeconds(60), 2)
        );

        List<MarketCandleResult> results = projector.rollupMinuteResults(duplicatedGapCandles, 3);

        assertThat(results).isEmpty();
    }

    private static List<HourlyMarketCandle> hourlyCandles(Instant bucketStart, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(offset -> hourly(bucketStart.plusSeconds(offset * 3600L), offset))
                .toList();
    }

    private static HourlyMarketCandle hourly(Instant openTime, int offset) {
        return new HourlyMarketCandle(
                1L,
                openTime,
                openTime.plusSeconds(3600),
                100 + offset,
                111 + offset,
                99 + offset,
                100.5 + offset,
                10,
                1005,
                openTime,
                openTime.plusSeconds(3600)
        );
    }

    private static MarketHistoryCandle minute(Instant openTime, int offset) {
        return new MarketHistoryCandle(
                1L,
                openTime,
                openTime.plusSeconds(60),
                100 + offset,
                101 + offset,
                99 + offset,
                100.5 + offset,
                10,
                1005
        );
    }
}
