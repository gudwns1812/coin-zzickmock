package coin.coinzzickmock.feature.market.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketHistoryCandleTest {
    @Test
    void mergesLatestPriceIntoExistingMinuteCandle() {
        MarketHistoryCandle candle = MarketHistoryCandle.first(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z"),
                101000
        ).mergeLatestPrice(102500);

        assertEquals(101000, candle.openPrice(), 0.0001);
        assertEquals(102500, candle.highPrice(), 0.0001);
        assertEquals(101000, candle.lowPrice(), 0.0001);
        assertEquals(102500, candle.closePrice(), 0.0001);
    }

    @Test
    void rollsUpHourlyCandleFromMinuteCandles() {
        Instant firstOpen = Instant.parse("2026-04-17T06:00:00Z");
        Instant firstClose = Instant.parse("2026-04-17T06:01:00Z");
        Instant secondOpen = Instant.parse("2026-04-17T06:01:00Z");
        Instant secondClose = Instant.parse("2026-04-17T06:02:00Z");

        HourlyMarketCandle hourly = HourlyMarketCandle.rollup(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T07:00:00Z"),
                List.of(
                        new MarketHistoryCandle(1L, firstOpen, firstClose, 101000, 102000, 100500, 101500, 1.2, 120000, 2),
                        new MarketHistoryCandle(1L, secondOpen, secondClose, 101500, 103000, 101000, 102700, 0.8, 80000, 1)
                )
        );

        assertEquals(101000, hourly.openPrice(), 0.0001);
        assertEquals(103000, hourly.highPrice(), 0.0001);
        assertEquals(100500, hourly.lowPrice(), 0.0001);
        assertEquals(102700, hourly.closePrice(), 0.0001);
        assertEquals(2.0, hourly.volume(), 0.0001);
        assertEquals(200000, hourly.quoteVolume(), 0.0001);
        assertEquals(3, hourly.tradeCount());
    }

    @Test
    void rollsUpHourlyCandleFromReverseOrderedMinuteCandles() {
        Instant firstOpen = Instant.parse("2026-04-17T06:00:00Z");
        Instant firstClose = Instant.parse("2026-04-17T06:01:00Z");
        Instant secondOpen = Instant.parse("2026-04-17T06:01:00Z");
        Instant secondClose = Instant.parse("2026-04-17T06:02:00Z");

        HourlyMarketCandle hourly = HourlyMarketCandle.rollup(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T07:00:00Z"),
                List.of(
                        new MarketHistoryCandle(1L, secondOpen, secondClose, 101500, 103000, 101000, 102700, 0.8, 80000, 1),
                        new MarketHistoryCandle(1L, firstOpen, firstClose, 101000, 102000, 100500, 101500, 1.2, 120000, 2)
                )
        );

        assertEquals(101000, hourly.openPrice(), 0.0001);
        assertEquals(102700, hourly.closePrice(), 0.0001);
        assertEquals(firstOpen, hourly.sourceMinuteOpenTime());
        assertEquals(secondClose, hourly.sourceMinuteCloseTime());
    }

    @Test
    void rejectsEmptyMinuteCandlesWhenRollingUpHourlyCandle() {
        assertThrows(IllegalArgumentException.class, () -> HourlyMarketCandle.rollup(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T07:00:00Z"),
                List.of()
        ));
    }

    @Test
    void revisesHourlyCandleWhenCurrentMinuteChanges() {
        MarketHistoryCandle previousMinute = MarketHistoryCandle.first(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z"),
                101000
        );
        MarketHistoryCandle revisedMinute = previousMinute.mergeLatestPrice(102500);

        HourlyMarketCandle hourly = HourlyMarketCandle.first(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T07:00:00Z"),
                previousMinute
        ).reviseMinute(previousMinute, revisedMinute);

        assertEquals(101000, hourly.openPrice(), 0.0001);
        assertEquals(102500, hourly.highPrice(), 0.0001);
        assertEquals(102500, hourly.closePrice(), 0.0001);
        assertEquals(Instant.parse("2026-04-17T06:01:00Z"), hourly.sourceMinuteCloseTime());
    }
}
