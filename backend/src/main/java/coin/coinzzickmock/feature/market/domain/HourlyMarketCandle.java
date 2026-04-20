package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;
import java.util.List;

public record HourlyMarketCandle(
        long symbolId,
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume,
        Instant sourceMinuteOpenTime,
        Instant sourceMinuteCloseTime
) {
    public static HourlyMarketCandle first(
            long symbolId,
            Instant openTime,
            Instant closeTime,
            MarketHistoryCandle minuteCandle
    ) {
        return new HourlyMarketCandle(
                symbolId,
                openTime,
                closeTime,
                minuteCandle.openPrice(),
                minuteCandle.highPrice(),
                minuteCandle.lowPrice(),
                minuteCandle.closePrice(),
                minuteCandle.volume(),
                minuteCandle.quoteVolume(),
                minuteCandle.openTime(),
                minuteCandle.closeTime()
        );
    }

    public static HourlyMarketCandle rollup(
            long symbolId,
            Instant openTime,
            Instant closeTime,
            List<MarketHistoryCandle> minuteCandles
    ) {
        if (minuteCandles == null || minuteCandles.isEmpty()) {
            throw new IllegalArgumentException("minuteCandles must not be empty");
        }

        MarketHistoryCandle first = minuteCandles.get(0);
        MarketHistoryCandle last = minuteCandles.get(0);

        double highPrice = first.highPrice();
        double lowPrice = first.lowPrice();
        double volume = 0.0;
        double quoteVolume = 0.0;

        for (MarketHistoryCandle candle : minuteCandles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            highPrice = Math.max(highPrice, candle.highPrice());
            lowPrice = Math.min(lowPrice, candle.lowPrice());
            volume += candle.volume();
            quoteVolume += candle.quoteVolume();
        }

        return new HourlyMarketCandle(
                symbolId,
                openTime,
                closeTime,
                first.openPrice(),
                highPrice,
                lowPrice,
                last.closePrice(),
                volume,
                quoteVolume,
                first.openTime(),
                last.closeTime()
        );
    }

    public HourlyMarketCandle reviseMinute(MarketHistoryCandle previousMinute, MarketHistoryCandle currentMinute) {
        double previousVolume = previousMinute == null ? 0.0 : previousMinute.volume();
        double previousQuoteVolume = previousMinute == null ? 0.0 : previousMinute.quoteVolume();

        boolean updatesEarliestMinute = currentMinute.openTime().isBefore(sourceMinuteOpenTime);
        boolean updatesLatestMinute = !currentMinute.closeTime().isBefore(sourceMinuteCloseTime);

        return new HourlyMarketCandle(
                symbolId,
                openTime,
                closeTime,
                updatesEarliestMinute ? currentMinute.openPrice() : openPrice,
                Math.max(highPrice, currentMinute.highPrice()),
                Math.min(lowPrice, currentMinute.lowPrice()),
                updatesLatestMinute ? currentMinute.closePrice() : closePrice,
                volume - previousVolume + currentMinute.volume(),
                quoteVolume - previousQuoteVolume + currentMinute.quoteVolume(),
                updatesEarliestMinute ? currentMinute.openTime() : sourceMinuteOpenTime,
                updatesLatestMinute ? currentMinute.closeTime() : sourceMinuteCloseTime
        );
    }
}
