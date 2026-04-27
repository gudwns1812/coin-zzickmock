package coin.coinzzickmock.feature.market.application.result;

import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.time.Instant;

public record MarketSummaryResult(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h,
        Instant serverTime,
        Instant nextFundingAt,
        int fundingIntervalHours
) {
    public MarketSummaryResult(
            String symbol,
            String displayName,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h
    ) {
        this(symbol, displayName, lastPrice, markPrice, indexPrice, fundingRate, change24h, defaultFundingTiming());
    }

    private MarketSummaryResult(
            String symbol,
            String displayName,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h,
            DefaultFundingTiming defaultFundingTiming
    ) {
        this(
                symbol,
                displayName,
                lastPrice,
                markPrice,
                indexPrice,
                fundingRate,
                change24h,
                defaultFundingTiming.serverTime(),
                defaultFundingTiming.nextFundingAt(),
                FundingSchedule.DEFAULT_INTERVAL_HOURS
        );
    }

    private static DefaultFundingTiming defaultFundingTiming() {
        Instant serverTime = Instant.now();
        return new DefaultFundingTiming(
                serverTime,
                FundingSchedule.defaultUsdtPerpetual().nextFundingAt(serverTime)
        );
    }

    private record DefaultFundingTiming(Instant serverTime, Instant nextFundingAt) {
    }
}
