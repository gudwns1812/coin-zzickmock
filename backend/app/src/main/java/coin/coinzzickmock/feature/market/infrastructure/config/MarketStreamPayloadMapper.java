package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.web.MarketCandleResponse;
import coin.coinzzickmock.feature.market.web.MarketSummaryResponse;
import java.util.Objects;

public final class MarketStreamPayloadMapper {
    private MarketStreamPayloadMapper() {
    }

    public static MarketSummaryResponse toResponse(MarketSummaryResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return MarketSummaryResponse.of(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.indexPrice(),
                result.fundingRate(),
                result.change24h(),
                result.turnover24hUsdt(),
                result.turnover24hUsdt(),
                result.serverTime(),
                result.nextFundingAt(),
                result.fundingIntervalHours()
        );
    }

    public static MarketCandleResponse toResponse(MarketCandleResult result) {
        Objects.requireNonNull(result, "result must not be null");
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
