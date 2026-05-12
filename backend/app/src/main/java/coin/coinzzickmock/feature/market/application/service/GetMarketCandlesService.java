package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleAppender;
import coin.coinzzickmock.feature.market.application.history.MarketHistoryLookupTelemetry;
import coin.coinzzickmock.feature.market.application.history.MarketPersistedCandleReader;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.util.List;
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

    public List<MarketCandleResult> getCandles(GetMarketCandlesQuery query) {
        MarketCandleInterval interval = MarketCandleInterval.from(query.interval());
        int limit = normalizeLimit(query.limit());
        long symbolId = resolveSymbolId(query.symbol());

        List<MarketCandleResult> persistedCandles = persistedCandleReader.read(
                symbolId,
                interval,
                limit,
                query.beforeOpenTime()
        );
        historyLookupTelemetry.recordDbLookup(query.symbol(), interval, query.beforeOpenTime(), persistedCandles, limit);
        return historicalCandleAppender.appendOlderCandles(
                query.symbol(),
                interval,
                query.beforeOpenTime(),
                limit,
                persistedCandles
        );
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
