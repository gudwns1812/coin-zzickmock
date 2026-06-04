package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleAppender;
import coin.coinzzickmock.feature.market.application.history.MarketHistoryLookupTelemetry;
import coin.coinzzickmock.feature.market.application.history.MarketPersistedCandleReader;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCache;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPage;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPolicy;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowSingleflight;
import coin.coinzzickmock.feature.market.application.latestwindow.RestVisibleCandleBoundary;
import coin.coinzzickmock.feature.market.application.latestwindow.RestVisibleCandleBoundaryResolver;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMarketCandlesService {
    private static final int DEFAULT_LIMIT = 120;
    private static final int MAX_LIMIT = 240;

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketPersistedCandleReader persistedCandleReader;
    private final MarketHistoricalCandleAppender historicalCandleAppender;
    private final MarketHistoryLookupTelemetry historyLookupTelemetry;
    private final RestVisibleCandleBoundaryResolver latestWindowBoundaryResolver;
    private final MarketLatestCandleWindowPolicy latestWindowPolicy;
    private final MarketLatestCandleWindowCache latestWindowCache;
    private final MarketLatestCandleWindowSingleflight latestWindowSingleflight;

    public List<MarketCandleResult> getCandles(GetMarketCandlesQuery query) {
        MarketCandleInterval interval = MarketCandleInterval.from(query.interval());
        int limit = normalizeLimit(query.limit());
        long symbolId = resolveSymbolId(query.symbol());

        if (!latestWindowPolicy.isEligible(query.beforeOpenTime(), limit)) {
            return loadFromCurrentPath(query.symbol(), symbolId, interval, query.beforeOpenTime(), limit);
        }

        Optional<RestVisibleCandleBoundary> boundary = latestWindowBoundaryResolver.resolve(symbolId, interval);
        if (boundary.isEmpty()) {
            return loadFromCurrentPath(query.symbol(), symbolId, interval, query.beforeOpenTime(), limit);
        }

        MarketLatestCandleWindowKey key = latestWindowPolicy.key(query.symbol(), boundary.get(), limit);
        MarketLatestCandleWindowCacheRead cacheRead = latestWindowCache.read(key);
        Optional<MarketLatestCandleWindowPage> cachedPage = cacheRead.page()
                .filter(page -> isUsableCachedPage(page, key));
        if (cachedPage.isPresent()) {
            historyLookupTelemetry.recordLatestWindowCache(
                    query.symbol(),
                    interval,
                    limit,
                    boundary.get().latestOutputOpenTime(),
                    "hit"
            );
            return cachedPage.get().candles();
        }

        historyLookupTelemetry.recordLatestWindowCache(
                query.symbol(),
                interval,
                limit,
                boundary.get().latestOutputOpenTime(),
                cacheRead.result()
        );
        return latestWindowSingleflight.load(key, () -> loadAndWriteLatestWindow(
                query.symbol(),
                symbolId,
                interval,
                limit,
                boundary.get(),
                key
        )).candles();
    }

    private MarketLatestCandleWindowPage loadAndWriteLatestWindow(
            String symbol,
            long symbolId,
            MarketCandleInterval interval,
            int limit,
            RestVisibleCandleBoundary boundary,
            MarketLatestCandleWindowKey key
    ) {
        List<MarketCandleResult> candles = loadFromCurrentPath(symbol, symbolId, interval, null, limit);
        MarketLatestCandleWindowPage page = new MarketLatestCandleWindowPage(
                candles,
                interval,
                limit,
                boundary.latestOutputOpenTime(),
                Instant.now()
        );

        if (shouldWriteLatestWindow(symbol, interval, limit, boundary, candles)
                && !latestWindowCache.write(key, page, latestWindowPolicy.ttl(boundary))) {
            historyLookupTelemetry.recordLatestWindowCache(
                    symbol,
                    interval,
                    limit,
                    boundary.latestOutputOpenTime(),
                    "write_unavailable"
            );
        }
        return page;
    }

    private List<MarketCandleResult> loadFromCurrentPath(
            String symbol,
            long symbolId,
            MarketCandleInterval interval,
            Instant beforeOpenTime,
            int limit
    ) {
        List<MarketCandleResult> persistedCandles = persistedCandleReader.read(
                symbolId,
                interval,
                limit,
                beforeOpenTime
        );
        historyLookupTelemetry.recordDbLookup(symbol, interval, beforeOpenTime, persistedCandles, limit);
        return historicalCandleAppender.appendOlderCandles(
                symbol,
                interval,
                beforeOpenTime,
                limit,
                persistedCandles
        );
    }

    private boolean shouldWriteLatestWindow(
            String symbol,
            MarketCandleInterval interval,
            int limit,
            RestVisibleCandleBoundary boundary,
            List<MarketCandleResult> candles
    ) {
        if (candles.isEmpty()) {
            historyLookupTelemetry.recordLatestWindowCache(
                    symbol,
                    interval,
                    limit,
                    boundary.latestOutputOpenTime(),
                    "skip_empty"
            );
            return false;
        }
        if (candles.size() > limit) {
            historyLookupTelemetry.recordLatestWindowCache(
                    symbol,
                    interval,
                    limit,
                    boundary.latestOutputOpenTime(),
                    "skip_oversized"
            );
            return false;
        }

        Instant latestReturnedOpenTime = candles.stream()
                .map(MarketCandleResult::openTime)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        if (!boundary.latestOutputOpenTime().equals(latestReturnedOpenTime)) {
            historyLookupTelemetry.recordLatestWindowCache(
                    symbol,
                    interval,
                    limit,
                    boundary.latestOutputOpenTime(),
                    "skip_boundary_mismatch"
            );
            return false;
        }
        return true;
    }

    private boolean isUsableCachedPage(MarketLatestCandleWindowPage page, MarketLatestCandleWindowKey key) {
        return page.interval() == key.interval()
                && page.limit() == key.limit()
                && key.latestOutputOpenTime().equals(page.latestOutputOpenTime())
                && page.candles().size() <= key.limit();
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private long resolveSymbolId(String symbol) {
        Long symbolId = marketHistoryRepository.findSymbolIdsBySymbols(List.of(symbol)).get(symbol);
        if (symbolId == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND);
        }
        return symbolId;
    }
}
