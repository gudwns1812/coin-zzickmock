package coin.coinzzickmock.feature.market.candle.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CompletedCalendarCandleBuilder {
    public Optional<CompletedMarketCandle> build(
            long symbolId,
            MarketCandleInterval interval,
            Instant bucketOpenTime,
            Instant bucketCloseTime,
            List<HourlyMarketCandle> hourlyCandles
    ) {
        if (!interval.isPersistedCalendarInterval() || bucketOpenTime == null || bucketCloseTime == null) {
            return Optional.empty();
        }
        if (!hasCompleteHourlyCoverage(bucketOpenTime, bucketCloseTime, hourlyCandles)) {
            return Optional.empty();
        }
        return Optional.of(CompletedMarketCandle.rollup(
                symbolId,
                interval,
                bucketOpenTime,
                bucketCloseTime,
                sortedHourlyCandles(hourlyCandles)
        ));
    }

    private boolean hasCompleteHourlyCoverage(
            Instant bucketOpenTime,
            Instant bucketCloseTime,
            List<HourlyMarketCandle> hourlyCandles
    ) {
        int expectedHours = expectedHours(bucketOpenTime, bucketCloseTime);
        if (hourlyCandles == null
                || hourlyCandles.size() != expectedHours
                || hourlyCandles.stream().anyMatch(this::hasMissingHourBoundary)) {
            return false;
        }
        List<HourlyMarketCandle> sortedHourlyCandles = sortedHourlyCandles(hourlyCandles);
        for (int index = 0; index < expectedHours; index++) {
            HourlyMarketCandle candle = sortedHourlyCandles.get(index);
            Instant expectedOpenTime = bucketOpenTime.plus(index, ChronoUnit.HOURS);
            if (!isSameSecond(candle.openTime(), expectedOpenTime)
                    || !isSameSecond(candle.closeTime(), expectedOpenTime.plus(1, ChronoUnit.HOURS))) {
                return false;
            }
        }
        return true;
    }

    private int expectedHours(Instant bucketOpenTime, Instant bucketCloseTime) {
        long hours = ChronoUnit.HOURS.between(bucketOpenTime, bucketCloseTime);
        if (hours <= 0 || hours > Integer.MAX_VALUE) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return (int) hours;
    }

    private List<HourlyMarketCandle> sortedHourlyCandles(List<HourlyMarketCandle> hourlyCandles) {
        return hourlyCandles.stream()
                .sorted(Comparator.comparing(HourlyMarketCandle::openTime))
                .toList();
    }

    private boolean hasMissingHourBoundary(HourlyMarketCandle candle) {
        return candle == null || candle.openTime() == null || candle.closeTime() == null;
    }

    private boolean isSameSecond(Instant actual, Instant expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return Objects.equals(actual.truncatedTo(ChronoUnit.SECONDS), expected.truncatedTo(ChronoUnit.SECONDS));
    }
}
