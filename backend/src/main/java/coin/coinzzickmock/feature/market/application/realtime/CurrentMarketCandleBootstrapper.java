package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CurrentMarketCandleBootstrapper {
    private static final Duration BOOTSTRAP_TTL = Duration.ofSeconds(5);

    private final MarketDataGateway marketDataGateway;
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final Clock clock;
    private final ConcurrentMap<BootstrapKey, CompletableFuture<Boolean>> inFlight = new ConcurrentHashMap<>();
    private final Map<BootstrapKey, Instant> lastAttempts = new ConcurrentHashMap<>();

    @Autowired
    public CurrentMarketCandleBootstrapper(
            MarketDataGateway marketDataGateway,
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector
    ) {
        this(marketDataGateway, realtimeMarketDataStore, realtimeMarketCandleProjector, Clock.systemUTC());
    }

    CurrentMarketCandleBootstrapper(
            MarketDataGateway marketDataGateway,
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            Clock clock
    ) {
        this.marketDataGateway = marketDataGateway;
        this.realtimeMarketDataStore = realtimeMarketDataStore;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.clock = clock;
    }

    public boolean bootstrapIfNeeded(String symbol, MarketCandleInterval selectedInterval) {
        if (realtimeMarketCandleProjector.latest(symbol, selectedInterval).isPresent()) {
            return false;
        }

        MarketCandleInterval sourceInterval = sourceInterval(selectedInterval);
        BootstrapKey key = new BootstrapKey(symbol, sourceInterval);
        Instant now = clock.instant();
        Instant lastAttempt = lastAttempts.get(key);
        if (lastAttempt != null && lastAttempt.plus(BOOTSTRAP_TTL).isAfter(now)) {
            return false;
        }

        CompletableFuture<Boolean> leader = new CompletableFuture<>();
        CompletableFuture<Boolean> existing = inFlight.putIfAbsent(key, leader);
        if (existing != null) {
            return existing.join();
        }

        try {
            boolean bootstrapped = bootstrap(symbol, sourceInterval, now);
            lastAttempts.put(key, now);
            leader.complete(bootstrapped);
            return bootstrapped;
        } catch (RuntimeException exception) {
            lastAttempts.put(key, now);
            leader.complete(false);
            log.debug("Current market candle bootstrap failed. symbol={} sourceInterval={}",
                    symbol, sourceInterval.value(), exception);
            return false;
        } finally {
            inFlight.remove(key, leader);
        }
    }

    private boolean bootstrap(String symbol, MarketCandleInterval sourceInterval, Instant now) {
        if (sourceInterval == MarketCandleInterval.ONE_MINUTE) {
            return bootstrapOneMinute(symbol, now);
        }

        return bootstrapOneHourFromMinutes(symbol, now) || bootstrapOneHourFromProvider(symbol, now);
    }

    private boolean bootstrapOneMinute(String symbol, Instant now) {
        Instant openTime = MarketTime.truncate(now, ChronoUnit.MINUTES);
        return loadProviderCandles(symbol, MarketCandleInterval.ONE_MINUTE, openTime, openTime.plus(1, ChronoUnit.MINUTES), 1)
                .stream()
                .filter(candle -> candle.openTime().equals(openTime))
                .findFirst()
                .map(candle -> acceptBootstrap(symbol, MarketCandleInterval.ONE_MINUTE, candle, now))
                .orElse(false);
    }

    private boolean bootstrapOneHourFromMinutes(String symbol, Instant now) {
        Instant hourOpenTime = MarketTime.truncate(now, ChronoUnit.HOURS);
        List<MarketHistoricalCandleSnapshot> minuteCandles = loadProviderCandles(
                symbol,
                MarketCandleInterval.ONE_MINUTE,
                hourOpenTime,
                hourOpenTime.plus(1, ChronoUnit.HOURS),
                60
        ).stream()
                .filter(candle -> !candle.openTime().isBefore(hourOpenTime))
                .filter(candle -> candle.openTime().isBefore(hourOpenTime.plus(1, ChronoUnit.HOURS)))
                .sorted(Comparator.comparing(MarketHistoricalCandleSnapshot::openTime))
                .toList();
        if (minuteCandles.isEmpty()) {
            return false;
        }

        MarketHistoricalCandleSnapshot first = minuteCandles.get(0);
        MarketHistoricalCandleSnapshot last = minuteCandles.get(minuteCandles.size() - 1);
        double high = minuteCandles.stream().mapToDouble(MarketHistoricalCandleSnapshot::highPrice).max().orElse(first.highPrice());
        double low = minuteCandles.stream().mapToDouble(MarketHistoricalCandleSnapshot::lowPrice).min().orElse(first.lowPrice());
        double volume = minuteCandles.stream().mapToDouble(MarketHistoricalCandleSnapshot::volume).sum();
        double quoteVolume = minuteCandles.stream().mapToDouble(MarketHistoricalCandleSnapshot::quoteVolume).sum();

        return realtimeMarketDataStore.acceptBootstrapCandle(new RealtimeMarketCandleUpdate(
                symbol,
                MarketCandleInterval.ONE_HOUR,
                hourOpenTime,
                decimal(first.openPrice()),
                decimal(high),
                decimal(low),
                decimal(last.closePrice()),
                decimal(volume),
                decimal(quoteVolume),
                decimal(quoteVolume),
                last.closeTime(),
                now
        ));
    }

    private boolean bootstrapOneHourFromProvider(String symbol, Instant now) {
        Instant openTime = MarketTime.truncate(now, ChronoUnit.HOURS);
        return loadProviderCandles(symbol, MarketCandleInterval.ONE_HOUR, openTime, openTime.plus(1, ChronoUnit.HOURS), 1)
                .stream()
                .filter(candle -> candle.openTime().equals(openTime))
                .findFirst()
                .map(candle -> acceptBootstrap(symbol, MarketCandleInterval.ONE_HOUR, candle, now))
                .orElse(false);
    }

    private List<MarketHistoricalCandleSnapshot> loadProviderCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        return marketDataGateway.loadHistoricalCandles(symbol, interval, fromInclusive, toExclusive, limit);
    }

    private boolean acceptBootstrap(
            String symbol,
            MarketCandleInterval interval,
            MarketHistoricalCandleSnapshot candle,
            Instant receivedAt
    ) {
        return realtimeMarketDataStore.acceptBootstrapCandle(new RealtimeMarketCandleUpdate(
                symbol,
                interval,
                candle.openTime(),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                decimal(candle.quoteVolume()),
                candle.closeTime(),
                receivedAt
        ));
    }

    private MarketCandleInterval sourceInterval(MarketCandleInterval selectedInterval) {
        return switch (selectedInterval) {
            case ONE_MINUTE, THREE_MINUTES, FIVE_MINUTES, FIFTEEN_MINUTES -> MarketCandleInterval.ONE_MINUTE;
            case ONE_HOUR, FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH -> MarketCandleInterval.ONE_HOUR;
        };
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private record BootstrapKey(String symbol, MarketCandleInterval sourceInterval) {
    }
}
