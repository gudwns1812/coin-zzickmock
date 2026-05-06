package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.providers.Providers;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketHistoricalCandleSegmentFetcher {
    private static final Duration PROVIDER_PERMIT_TIMEOUT = Duration.ofSeconds(3);

    private final Providers providers;
    private final MarketHistoricalCandleTelemetry telemetry;
    private final Semaphore providerLane = new Semaphore(1);
    private final ConcurrentMap<String, CompletableFuture<List<MarketCandleResult>>> fills = new ConcurrentHashMap<>();

    public MarketHistoricalCandleSegmentFetcher(
            Providers providers,
            MarketHistoricalCandleTelemetry telemetry
    ) {
        this.providers = providers;
        this.telemetry = telemetry;
    }

    public List<MarketCandleResult> fetch(MarketHistoricalCandleSegment segment) {
        CompletableFuture<List<MarketCandleResult>> fill = new CompletableFuture<>();
        CompletableFuture<List<MarketCandleResult>> existing = fills.putIfAbsent(segment.cacheKey(), fill);
        if (existing != null) {
            return existing.join();
        }

        try {
            List<MarketCandleResult> candles = fetchFromProvider(segment);
            fill.complete(candles);
            return candles;
        } catch (RuntimeException exception) {
            fill.complete(List.of());
            throw exception;
        } finally {
            fills.remove(segment.cacheKey());
        }
    }

    private List<MarketCandleResult> fetchFromProvider(MarketHistoricalCandleSegment segment) {
        boolean acquired = false;
        try {
            acquired = providerLane.tryAcquire(PROVIDER_PERMIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                telemetry.record("market.history.bitget.timeout", segment, "bitget", "timeout");
                log.warn("Timed out waiting for Bitget historical lane. provider=bitget symbol={} interval={} rangeBucket={}",
                        segment.symbol(), segment.interval().value(), rangeBucket(segment));
                return List.of();
            }

            telemetry.record("market.history.bitget.request", segment, "bitget", "request");
            List<MarketCandleResult> candles = providers.connector()
                    .marketDataGateway()
                    .loadHistoricalCandles(
                            segment.symbol(),
                            segment.interval(),
                            segment.startInclusive(),
                            segment.endExclusive(),
                            segment.size()
                    )
                    .stream()
                    .map(this::toResult)
                    .filter(candle -> !candle.openTime().isBefore(segment.startInclusive()))
                    .filter(candle -> candle.openTime().isBefore(segment.endExclusive()))
                    .sorted(Comparator.comparing(MarketCandleResult::openTime))
                    .toList();

            telemetry.record(
                    candles.isEmpty() ? "market.history.bitget.empty" : "market.history.bitget.success",
                    segment,
                    "bitget",
                    candles.isEmpty() ? "empty" : "success"
            );
            return candles;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            telemetry.record("market.history.bitget.timeout", segment, "bitget", "interrupted");
            return List.of();
        } catch (RuntimeException exception) {
            telemetry.record("market.history.bitget.failure", segment, "bitget", "failure");
            log.warn("Failed to load Bitget historical candles. provider=bitget symbol={} interval={} rangeBucket={}",
                    segment.symbol(), segment.interval().value(), rangeBucket(segment), exception);
            return List.of();
        } finally {
            if (acquired) {
                providerLane.release();
            }
        }
    }

    private String rangeBucket(MarketHistoricalCandleSegment segment) {
        return segment.granularity() + ":size" + segment.size();
    }

    private MarketCandleResult toResult(MarketHistoricalCandleSnapshot candle) {
        return new MarketCandleResult(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume()
        );
    }
}
