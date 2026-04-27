package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.time.Instant;

public record MarketSummaryResponse(
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
    public static MarketSummaryResponse from(MarketSummaryResult result) {
        return new MarketSummaryResponse(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.indexPrice(),
                result.fundingRate(),
                result.change24h(),
                result.serverTime(),
                result.nextFundingAt(),
                result.fundingIntervalHours()
        );
    }
}
