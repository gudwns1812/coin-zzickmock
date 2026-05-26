package coin.coinzzickmock.feature.market.application.query;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.dto.MarketRealtimeSourceSnapshot;
import coin.coinzzickmock.feature.market.application.dto.RealtimeMarketTickerSnapshot;
import coin.coinzzickmock.feature.market.application.implement.MarketRealtimeFreshnessPolicy;
import coin.coinzzickmock.feature.market.application.implement.RealtimeMarketDataStore;
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
                .orElseThrow(() -> unavailableMarketException(symbol));
    }

    public Optional<MarketSnapshot> freshMarket(String symbol) {
        Instant now = Instant.now();
        MarketRealtimeFreshnessPolicy policy = new MarketRealtimeFreshnessPolicy(DEFAULT_FRESHNESS, false);

        return realtimeMarketDataStore.latestTicker(symbol)
                .filter(state -> policy.accepts(state.source(), now))
                .map(tickerState -> toMarketSnapshot(symbol, tickerState));
    }

    public Optional<Double> freshMarkPrice(String symbol) {
        MarketRealtimeFreshnessPolicy policy = new MarketRealtimeFreshnessPolicy(DEFAULT_FRESHNESS, false);
        Instant now = Instant.now();
        return realtimeMarketDataStore.latestTicker(symbol)
                .filter(ticker -> policy.accepts(ticker.source(), now))
                .map(ticker -> ticker.markPrice().doubleValue());
    }

    private MarketSnapshot toMarketSnapshot(String symbol, RealtimeMarketTickerSnapshot ticker) {
        return new MarketSnapshot(
                symbol,
                symbol,
                ticker.lastPrice().doubleValue(),
                ticker.markPrice().doubleValue(),
                ticker.indexPrice().doubleValue(),
                ticker.fundingRate() == null ? 0.0 : ticker.fundingRate().doubleValue(),
                0.0,
                0.0
        );
    }

    private CoreException unavailableMarketException(String symbol) {
        Instant now = Instant.now();
        MarketRealtimeFreshnessPolicy policy = new MarketRealtimeFreshnessPolicy(DEFAULT_FRESHNESS, false);
        Optional<RealtimeMarketTickerSnapshot> ticker = realtimeMarketDataStore.latestTicker(symbol);
        boolean hasFreshTicker = ticker.map(state -> policy.accepts(state.source(), now)).orElse(false);

        log.warn(
                "Fresh realtime market price is unavailable. symbol={} reason={} tickerPresent={} tickerAgeMs={} tickerReceivedAt={} tickerSourceEventTime={}",
                symbol,
                unavailableReason(ticker, hasFreshTicker, now),
                ticker.isPresent(),
                ticker.map(state -> ageMs(state.source(), now)).orElse(null),
                ticker.map(state -> state.source().receivedAt()).orElse(null),
                ticker.map(state -> state.source().sourceEventTime()).orElse(null)
        );
        return new CoreException(unavailableErrorCode(ticker, hasFreshTicker));
    }

    private ErrorCode unavailableErrorCode(
            Optional<RealtimeMarketTickerSnapshot> ticker,
            boolean hasFreshTicker
    ) {
        if (ticker.isEmpty()) {
            return ErrorCode.MARKET_NOT_FOUND;
        }
        if (!hasFreshTicker) {
            return ErrorCode.MARKET_PRICE_STALE;
        }
        return ErrorCode.MARKET_NOT_FOUND;
    }

    private String unavailableReason(
            Optional<RealtimeMarketTickerSnapshot> ticker,
            boolean hasFreshTicker,
            Instant now
    ) {
        if (ticker.isEmpty()) {
            return "ticker_missing";
        }
        if (hasFreshTicker) {
            return "ticker_available";
        }
        if (ticker.filter(state -> state.source().isFresh(now, DEFAULT_FRESHNESS)).isPresent()) {
            return "ticker_non_execution_source";
        }
        return "ticker_stale";
    }

    private long ageMs(MarketRealtimeSourceSnapshot source, Instant now) {
        return source.ageMs(now);
    }
}
