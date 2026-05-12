package coin.coinzzickmock.feature.market.web;

import java.time.Instant;

public record MarketSummaryResponse(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h,
        double turnover24hUsdt,
        double volume24h,
        Instant serverTime,
        Instant nextFundingAt,
        int fundingIntervalHours
) {
    public static MarketSummaryResponse of(
            String symbol,
            String displayName,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h,
            double turnover24hUsdt,
            Instant serverTime,
            Instant nextFundingAt,
            int fundingIntervalHours
    ) {
        return new MarketSummaryResponse(
                symbol,
                displayName,
                lastPrice,
                markPrice,
                indexPrice,
                fundingRate,
                change24h,
                turnover24hUsdt,
                turnover24hUsdt,
                serverTime,
                nextFundingAt,
                fundingIntervalHours
        );
    }
}
