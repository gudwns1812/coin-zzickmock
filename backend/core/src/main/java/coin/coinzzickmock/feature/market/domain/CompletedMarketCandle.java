package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;
import java.util.List;

public record CompletedMarketCandle(
        long symbolId,
        MarketCandleInterval interval,
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume
) {
    public static CompletedMarketCandle fromHourly(HourlyMarketCandle candle) {
        return new CompletedMarketCandle(
                candle.symbolId(),
                MarketCandleInterval.ONE_HOUR,
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume(),
                candle.quoteVolume()
        );
    }

    public static CompletedMarketCandle rollup(
            long symbolId,
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime,
            List<HourlyMarketCandle> hourlyCandles
    ) {
        if (hourlyCandles == null || hourlyCandles.isEmpty()) {
            throw new IllegalArgumentException("hourlyCandles must not be empty");
        }
        HourlyMarketCandle first = hourlyCandles.get(0);
        HourlyMarketCandle last = hourlyCandles.get(0);
        double highPrice = first.highPrice();
        double lowPrice = first.lowPrice();
        double volume = 0.0;
        double quoteVolume = 0.0;
        for (HourlyMarketCandle candle : hourlyCandles) {
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
        return new CompletedMarketCandle(
                symbolId,
                interval,
                openTime,
                closeTime,
                first.openPrice(),
                highPrice,
                lowPrice,
                last.closePrice(),
                volume,
                quoteVolume
        );
    }

    public HourlyMarketCandle toHourlyMarketCandle() {
        if (interval != MarketCandleInterval.ONE_HOUR) {
            throw new IllegalStateException("Only ONE_HOUR completed candles can be viewed as HourlyMarketCandle");
        }
        return new HourlyMarketCandle(
                symbolId,
                openTime,
                closeTime,
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume,
                quoteVolume,
                openTime,
                closeTime
        );
    }
}
