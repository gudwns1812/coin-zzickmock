package coin.coinzzickmock.feature.market.application.query;

import coin.coinzzickmock.feature.market.application.implement.MarketRealtimeFreshnessPolicy;
import coin.coinzzickmock.feature.market.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.dto.MarketRealtimeSourceSnapshot;
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
        Optional<RealtimeMarketDataStore.RealtimeMarketTradeState> trade = realtimeMarketDataStore.latestTrade(symbol);
        Optional<RealtimeMarketDataStore.RealtimeMarketTickerState> ticker = realtimeMarketDataStore.latestTicker(symbol);
        Optional<RealtimeMarketDataStore.RealtimeMarketTickerState> freshTicker =
                ticker.filter(state -> policy.accepts(state.source(), now));
        Optional<RealtimeMarketDataStore.RealtimeMarketTradeState> freshTrade =
                trade.filter(state -> policy.accepts(state.source(), now));

        return freshTicker.map(tickerState -> toMarketSnapshot(symbol, freshTrade, tickerState));
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
            Optional<RealtimeMarketDataStore.RealtimeMarketTradeState> trade,
            RealtimeMarketDataStore.RealtimeMarketTickerState ticker
    ) {
        return new MarketSnapshot(
                symbol,
                symbol,
                trade.map(state -> state.price().doubleValue())
                        .orElseGet(() -> ticker.lastPrice().doubleValue()),
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
        Optional<RealtimeMarketDataStore.RealtimeMarketTradeState> trade = realtimeMarketDataStore.latestTrade(symbol);
        Optional<RealtimeMarketDataStore.RealtimeMarketTickerState> ticker = realtimeMarketDataStore.latestTicker(symbol);
        boolean hasFreshTrade = trade.map(state -> policy.accepts(state.source(), now)).orElse(false);
        boolean hasFreshTicker = ticker.map(state -> policy.accepts(state.source(), now)).orElse(false);

        log.warn(
                "Fresh realtime market price is unavailable. symbol={} reason={} tradePresent={} tradeAgeMs={} tradeReceivedAt={} tradeSourceEventTime={} tickerPresent={} tickerAgeMs={} tickerReceivedAt={} tickerSourceEventTime={}",
                symbol,
                unavailableReason(trade.isPresent(), hasFreshTrade, ticker.isPresent(), hasFreshTicker),
                trade.isPresent(),
                trade.map(state -> ageMs(state.source(), now)).orElse(null),
                trade.map(state -> state.source().receivedAt()).orElse(null),
                trade.map(state -> state.source().sourceEventTime()).orElse(null),
                ticker.isPresent(),
                ticker.map(state -> ageMs(state.source(), now)).orElse(null),
                ticker.map(state -> state.source().receivedAt()).orElse(null),
                ticker.map(state -> state.source().sourceEventTime()).orElse(null)
        );
        return new CoreException(ErrorCode.MARKET_NOT_FOUND);
    }

    private String unavailableReason(
            boolean tradePresent,
            boolean hasFreshTrade,
            boolean tickerPresent,
            boolean hasFreshTicker
    ) {
        if (!tradePresent && !tickerPresent) {
            return "trade_and_ticker_missing";
        }
        if (!tradePresent) {
            return hasFreshTicker ? "trade_missing" : "trade_missing_ticker_stale";
        }
        if (!tickerPresent) {
            return hasFreshTrade ? "ticker_missing" : "ticker_missing_trade_stale";
        }
        if (!hasFreshTrade && !hasFreshTicker) {
            return "trade_and_ticker_stale";
        }
        if (!hasFreshTrade) {
            return "trade_stale";
        }
        return "ticker_stale";
    }

    private long ageMs(MarketRealtimeSourceSnapshot source, Instant now) {
        return source.ageMs(now);
    }
}
