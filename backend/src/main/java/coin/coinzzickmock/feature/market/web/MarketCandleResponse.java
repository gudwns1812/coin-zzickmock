package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import java.time.Instant;

public record MarketCandleResponse(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume
) {
    public static MarketCandleResponse from(MarketCandleResult result) {
        return new MarketCandleResponse(
                result.openTime(),
                result.closeTime(),
                result.openPrice(),
                result.highPrice(),
                result.lowPrice(),
                result.closePrice(),
                result.volume()
        );
    }
}
