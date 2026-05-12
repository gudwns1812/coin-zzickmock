package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeMarketCandleProjector {
    private final RealtimeMarketDataStore realtimeMarketDataStore;

    public java.util.Optional<MarketCandleResult> latest(String symbol, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE, ONE_HOUR -> realtimeMarketDataStore.latestCandle(symbol, interval)
                    .map(this::toResult);
            case THREE_MINUTES -> latestMinuteRollup(symbol, interval, 3);
            case FIVE_MINUTES -> latestMinuteRollup(symbol, interval, 5);
            case FIFTEEN_MINUTES -> latestMinuteRollup(symbol, interval, 15);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH -> latestHourlyRollup(symbol, interval);
        };
    }

    private java.util.Optional<MarketCandleResult> latestMinuteRollup(
            String symbol,
            MarketCandleInterval interval,
            int bucketMinutes
    ) {
        return realtimeMarketDataStore.latestCandle(symbol, MarketCandleInterval.ONE_MINUTE)
                .flatMap(latest -> {
                    Instant bucketStart = MarketTime.alignToMinuteBucket(latest.openTime(), bucketMinutes);
                    List<RealtimeMarketCandleState> candles = realtimeMarketDataStore.candles(
                            symbol,
                            MarketCandleInterval.ONE_MINUTE,
                            bucketStart,
                            bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES)
                    );
                    return candles.isEmpty()
                            ? java.util.Optional.empty()
                            : java.util.Optional.of(rollup(
                                    bucketStart,
                                    bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES),
                                    candles
                            ));
                });
    }

    private java.util.Optional<MarketCandleResult> latestHourlyRollup(String symbol, MarketCandleInterval interval) {
        return realtimeMarketDataStore.latestCandle(symbol, MarketCandleInterval.ONE_HOUR)
                .flatMap(latest -> {
                    Instant bucketStart = MarketTime.bucketStart(latest.openTime(), interval);
                    List<RealtimeMarketCandleState> candles = realtimeMarketDataStore.candles(
                            symbol,
                            MarketCandleInterval.ONE_HOUR,
                            bucketStart,
                            MarketTime.bucketClose(bucketStart, interval)
                    );
                    return candles.isEmpty()
                            ? java.util.Optional.empty()
                            : java.util.Optional.of(rollup(bucketStart, MarketTime.bucketClose(bucketStart, interval), candles));
                });
    }

    private MarketCandleResult rollup(Instant openTime, Instant closeTime, List<RealtimeMarketCandleState> candles) {
        RealtimeMarketCandleState first = candles.get(0);
        RealtimeMarketCandleState last = candles.get(0);
        BigDecimal high = first.highPrice();
        BigDecimal low = first.lowPrice();
        BigDecimal volume = BigDecimal.ZERO;

        for (RealtimeMarketCandleState candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = high.max(candle.highPrice());
            low = low.min(candle.lowPrice());
            volume = volume.add(candle.baseVolume());
        }

        return new MarketCandleResult(
                openTime,
                closeTime,
                first.openPrice().doubleValue(),
                high.doubleValue(),
                low.doubleValue(),
                last.closePrice().doubleValue(),
                volume.doubleValue()
        );
    }

    private MarketCandleResult toResult(RealtimeMarketCandleState candle) {
        return new MarketCandleResult(
                candle.openTime(),
                closeTime(candle),
                candle.openPrice().doubleValue(),
                candle.highPrice().doubleValue(),
                candle.lowPrice().doubleValue(),
                candle.closePrice().doubleValue(),
                candle.baseVolume().doubleValue()
        );
    }

    private Instant closeTime(RealtimeMarketCandleState candle) {
        return switch (candle.interval()) {
            case ONE_MINUTE -> candle.openTime().plus(1, ChronoUnit.MINUTES);
            case ONE_HOUR -> candle.openTime().plus(1, ChronoUnit.HOURS);
            default -> MarketTime.bucketClose(candle.openTime(), candle.interval());
        };
    }
}
