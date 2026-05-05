package coin.coinzzickmock.feature.market.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketHistoryStartupBackfillTest {
    @Test
    void backfillsMissingMinuteCandlesFromDbTrackedCursorOnly() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 103000, 102950, 102900, 0.00013, 4.4)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:01:00Z", 101200, 101500, 101100, 101400, 12.0, 120000.0),
                minuteCandle("2026-04-17T06:02:00Z", 101400, 102000, 101300, 101900, 14.0, 140000.0)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z"),
                101000,
                101000,
                101000,
                101000,
                0.0,
                0.0
        ));
        MarketHistoryStartupBackfill marketHistoryStartupBackfill =
                new MarketHistoryStartupBackfill(
                        marketHistoryRepository,
                        new MarketHistoryRecorder(marketHistoryRepository)
                );

        marketHistoryStartupBackfill.backfillMissingMinuteHistory(
                Instant.parse("2026-04-17T06:03:20Z"),
                marketDataGateway
        );

        assertEquals(1, marketDataGateway.minuteHistoryCalls());
        assertEquals(3, marketHistoryRepository.minuteCandleCount());
        assertEquals(1, marketHistoryRepository.hourlyCandleCount());
        assertEquals(101900, marketHistoryRepository.hourlyCandle(1L, "2026-04-17T06:00:00Z").closePrice(), 0.0001);
    }

    @Test
    void skipsBackfillWhenNoLatestPersistedMinuteExists() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 103000, 102950, 102900, 0.00013, 4.4)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:01:00Z", 101200, 101500, 101100, 101400, 12.0, 120000.0)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        MarketHistoryStartupBackfill marketHistoryStartupBackfill =
                new MarketHistoryStartupBackfill(
                        marketHistoryRepository,
                        new MarketHistoryRecorder(marketHistoryRepository)
                );

        marketHistoryStartupBackfill.backfillMissingMinuteHistory(
                Instant.parse("2026-04-17T06:03:20Z"),
                marketDataGateway
        );

        assertEquals(0, marketDataGateway.minuteHistoryCalls());
        assertEquals(0, marketHistoryRepository.minuteCandleCount());
        assertEquals(0, marketHistoryRepository.hourlyCandleCount());
    }

    @Test
    void ignoresFutureDatedLatestCandleWhenResolvingBackfillCursor() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 103000, 102950, 102900, 0.00013, 4.4)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:01:00Z", 101200, 101500, 101100, 101400, 12.0, 120000.0),
                minuteCandle("2026-04-17T06:02:00Z", 101400, 102000, 101300, 101900, 14.0, 140000.0)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                1L,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z"),
                101000,
                101000,
                101000,
                101000,
                0.0,
                0.0
        ));
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                1L,
                Instant.parse("2026-04-17T15:00:00Z"),
                Instant.parse("2026-04-17T15:01:00Z"),
                110000,
                110000,
                110000,
                110000,
                0.0,
                0.0
        ));
        MarketHistoryStartupBackfill marketHistoryStartupBackfill =
                new MarketHistoryStartupBackfill(
                        marketHistoryRepository,
                        new MarketHistoryRecorder(marketHistoryRepository)
                );

        marketHistoryStartupBackfill.backfillMissingMinuteHistory(
                Instant.parse("2026-04-17T06:03:20Z"),
                marketDataGateway
        );

        assertEquals(1, marketDataGateway.minuteHistoryCalls());
        assertEquals(4, marketHistoryRepository.minuteCandleCount());
        assertEquals(101900, marketHistoryRepository.hourlyCandle(1L, "2026-04-17T06:00:00Z").closePrice(), 0.0001);
    }

    private static MarketSnapshot snapshot(
            String symbol,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h
    ) {
        return new MarketSnapshot(symbol, symbol + " Perpetual", lastPrice, markPrice, indexPrice, fundingRate, change24h);
    }

    private static MarketMinuteCandleSnapshot minuteCandle(
            String openTime,
            double openPrice,
            double highPrice,
            double lowPrice,
            double closePrice,
            double volume,
            double quoteVolume
    ) {
        Instant candleOpenTime = Instant.parse(openTime);
        return new MarketMinuteCandleSnapshot(
                candleOpenTime,
                candleOpenTime.plus(1, ChronoUnit.MINUTES),
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume,
                quoteVolume
        );
    }

    private static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private final List<MarketSnapshot> supportedMarkets;
        private final Map<String, List<MarketMinuteCandleSnapshot>> minuteCandles = new LinkedHashMap<>();
        private int minuteHistoryCalls;

        private FakeMarketDataGateway(List<MarketSnapshot> supportedMarkets) {
            this.supportedMarkets = new ArrayList<>(supportedMarkets);
        }

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.copyOf(supportedMarkets);
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return supportedMarkets.stream()
                    .filter(snapshot -> snapshot.symbol().equals(symbol))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(String symbol, Instant fromInclusive, Instant toExclusive) {
            minuteHistoryCalls++;
            return minuteCandles.getOrDefault(symbol, List.of()).stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        private void replaceMinuteCandles(String symbol, List<MarketMinuteCandleSnapshot> candles) {
            minuteCandles.put(symbol, new ArrayList<>(candles));
        }

        private int minuteHistoryCalls() {
            return minuteHistoryCalls;
        }
    }

    private static class InMemoryMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final Map<String, Long> symbolIds = Map.of("BTCUSDT", 1L, "ETHUSDT", 2L);
        private final Map<String, MarketHistoryCandle> minuteCandles = new LinkedHashMap<>();
        private final Map<String, HourlyMarketCandle> hourlyCandles = new LinkedHashMap<>();

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
            Map<String, Long> resolved = new LinkedHashMap<>();
            symbols.forEach(symbol -> {
                if (symbolIds.containsKey(symbol)) {
                    resolved.put(symbol, symbolIds.get(symbol));
                }
            });
            return resolved;
        }

        @Override
        public List<StartupBackfillCursor> findStartupBackfillCursors() {
            return symbolIds.entrySet().stream()
                    .map(entry -> new StartupBackfillCursor(
                            entry.getValue(),
                            entry.getKey(),
                            findLatestMinuteCandleOpenTime(entry.getValue()).orElse(null)
                    ))
                    .toList();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(MarketHistoryCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(MarketHistoryCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(HourlyMarketCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(HourlyMarketCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(minuteCandles.get(key(symbolId, openTime)));
        }

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(MarketHistoryCandle::openTime))
                    .toList();
        }

        @Override
        public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(hourlyCandles.get(key(symbolId, openTime)));
        }

        @Override
        public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(HourlyMarketCandle::openTime))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            minuteCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            hourlyCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        private HourlyMarketCandle hourlyCandle(long symbolId, String openTime) {
            return hourlyCandles.get(key(symbolId, Instant.parse(openTime)));
        }

        private int minuteCandleCount() {
            return minuteCandles.size();
        }

        private int hourlyCandleCount() {
            return hourlyCandles.size();
        }

        private String key(long symbolId, Instant openTime) {
            return symbolId + ":" + openTime;
        }
    }
}
