package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CompletedHourlyCandleBuilder {
    private static final int EXPECTED_MINUTES_PER_HOUR = 60;

    public Optional<HourlyMarketCandle> build(
            long symbolId,
            Instant hourlyOpenTime,
            Instant hourlyCloseTime,
            List<MarketHistoryCandle> minuteCandles
    ) {
        if (hourlyOpenTime == null || hourlyCloseTime == null) {
            return Optional.empty();
        }
        if (!hasCompleteMinuteCoverage(hourlyOpenTime, hourlyCloseTime, minuteCandles)) {
            return Optional.empty();
        }

        return Optional.of(HourlyMarketCandle.rollup(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime,
                sortedMinuteCandles(minuteCandles)
        ));
    }

    private boolean hasCompleteMinuteCoverage(
            Instant hourlyOpenTime,
            Instant hourlyCloseTime,
            List<MarketHistoryCandle> minuteCandles
    ) {
        if (minuteCandles == null
                || minuteCandles.size() != EXPECTED_MINUTES_PER_HOUR
                || minuteCandles.stream().anyMatch(this::hasMissingMinuteBoundary)) {
            return false;
        }
        if (!isSameSecond(hourlyCloseTime, hourlyOpenTime.plus(1, ChronoUnit.HOURS))) {
            return false;
        }

        List<MarketHistoryCandle> sortedMinuteCandles = sortedMinuteCandles(minuteCandles);
        for (int index = 0; index < EXPECTED_MINUTES_PER_HOUR; index++) {
            MarketHistoryCandle candle = sortedMinuteCandles.get(index);
            Instant expectedOpenTime = hourlyOpenTime.plus(index, ChronoUnit.MINUTES);
            if (!isSameSecond(candle.openTime(), expectedOpenTime)
                    || !isSameSecond(candle.closeTime(), expectedOpenTime.plus(1, ChronoUnit.MINUTES))) {
                return false;
            }
        }

        return true;
    }

    private List<MarketHistoryCandle> sortedMinuteCandles(List<MarketHistoryCandle> minuteCandles) {
        return minuteCandles.stream()
                .sorted(Comparator.comparing(MarketHistoryCandle::openTime))
                .toList();
    }

    private boolean hasMissingMinuteBoundary(MarketHistoryCandle candle) {
        return candle == null || candle.openTime() == null || candle.closeTime() == null;
    }

    private boolean isSameSecond(Instant actual, Instant expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return Objects.equals(
                actual.truncatedTo(ChronoUnit.SECONDS),
                expected.truncatedTo(ChronoUnit.SECONDS)
        );
    }
}
