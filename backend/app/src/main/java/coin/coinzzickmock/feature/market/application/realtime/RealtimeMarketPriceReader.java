package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeMarketPriceReader {
    private static final Duration DEFAULT_FRESHNESS = Duration.ofSeconds(10);

    private final RealtimeMarketDataStore realtimeMarketDataStore;

    public MarketSnapshot requireFreshMarket(String symbol) {
        return freshMarket(symbol)
                .orElseThrow(() -> {
                    log.debug("Fresh realtime market price is unavailable. symbol={} reason=stale_or_missing", symbol);
                    return new CoreException(ErrorCode.MARKET_NOT_FOUND);
                });
    }

    public Optional<MarketSnapshot> freshMarket(String symbol) {
        MarketRealtimeFreshnessPolicy policy = new MarketRealtimeFreshnessPolicy(DEFAULT_FRESHNESS, false);
        Instant now = Instant.now();
        return realtimeMarketDataStore.latestTrade(symbol)
                .filter(trade -> policy.accepts(trade.source(), now))
                .flatMap(trade -> realtimeMarketDataStore.latestTicker(symbol)
                        .filter(ticker -> policy.accepts(ticker.source(), now))
                        .map(ticker -> toMarketSnapshot(symbol, trade, ticker)));
    }

    public Optional<Double> freshMarkPrice(String symbol) {
        MarketRealtimeFreshnessPolicy policy = new MarketRealtimeFreshnessPolicy(DEFAULT_FRESHNESS, false);
        Instant now = Instant.now();
        return realtimeMarketDataStore.latestTicker(symbol)
                .filter(ticker -> policy.accepts(ticker.source(), now))
                .map(ticker -> ticker.markPrice().doubleValue());
    }

    private MarketSnapshot toMarketSnapshot(
            String symbol,
            RealtimeMarketDataStore.RealtimeMarketTradeState trade,
            RealtimeMarketDataStore.RealtimeMarketTickerState ticker
    ) {
        return new MarketSnapshot(
                symbol,
                symbol,
                trade.price().doubleValue(),
                ticker.markPrice().doubleValue(),
                ticker.indexPrice().doubleValue(),
                ticker.fundingRate() == null ? 0.0 : ticker.fundingRate().doubleValue(),
                0.0,
                0.0
        );
    }
}
