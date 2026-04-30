package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeMarketSummaryProjector {
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final MarketFundingScheduleLookup marketFundingScheduleLookup;

    public Optional<MarketSummaryResult> project(String symbol, MarketSummaryResult previous) {
        return realtimeMarketDataStore.latestTicker(symbol)
                .map(ticker -> toSummary(symbol, ticker, previous));
    }

    private MarketSummaryResult toSummary(
            String symbol,
            RealtimeMarketDataStore.RealtimeMarketTickerState ticker,
            MarketSummaryResult previous
    ) {
        Instant serverTime = Instant.now();
        FundingSchedule fundingSchedule = marketFundingScheduleLookup.scheduleFor(symbol);
        double lastPrice = realtimeMarketDataStore.latestTrade(symbol)
                .map(trade -> trade.price().doubleValue())
                .orElseGet(() -> ticker.lastPrice().doubleValue());

        return new MarketSummaryResult(
                symbol,
                displayName(symbol, previous),
                lastPrice,
                ticker.markPrice().doubleValue(),
                ticker.indexPrice().doubleValue(),
                fundingRate(ticker, previous),
                previous == null ? 0.0 : previous.change24h(),
                previous == null ? 0.0 : previous.turnover24hUsdt(),
                serverTime,
                nextFundingAt(ticker, fundingSchedule, serverTime),
                fundingSchedule.intervalHours()
        );
    }

    private String displayName(String symbol, MarketSummaryResult previous) {
        if (previous == null || previous.displayName() == null || previous.displayName().isBlank()) {
            return symbol;
        }
        return previous.displayName();
    }

    private double fundingRate(RealtimeMarketDataStore.RealtimeMarketTickerState ticker, MarketSummaryResult previous) {
        if (ticker.fundingRate() != null) {
            return ticker.fundingRate().doubleValue();
        }
        return previous == null ? 0.0 : previous.fundingRate();
    }

    private Instant nextFundingAt(
            RealtimeMarketDataStore.RealtimeMarketTickerState ticker,
            FundingSchedule fundingSchedule,
            Instant serverTime
    ) {
        if (ticker.nextFundingTime() != null) {
            return ticker.nextFundingTime();
        }
        return fundingSchedule.nextFundingAt(serverTime);
    }
}
