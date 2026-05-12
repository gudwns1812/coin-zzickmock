package coin.coinzzickmock.feature.market.web;

import java.time.Instant;
import java.util.List;

public record MarketCandleHistoryFinalizedResponse(
        String type,
        String symbol,
        Instant openTime,
        Instant closeTime,
        List<String> affectedIntervals
) {
    private static final String TYPE = "historyFinalized";

    public static MarketCandleHistoryFinalizedResponse of(
            String symbol,
            Instant openTime,
            Instant closeTime,
            List<String> affectedIntervals
    ) {
        return new MarketCandleHistoryFinalizedResponse(
                TYPE,
                symbol,
                openTime,
                closeTime,
                List.copyOf(affectedIntervals)
        );
    }
}
