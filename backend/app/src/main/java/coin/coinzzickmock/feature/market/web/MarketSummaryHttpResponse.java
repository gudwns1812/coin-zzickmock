package coin.coinzzickmock.feature.market.web;

import java.time.Instant;

public record MarketSummaryHttpResponse(
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
}
