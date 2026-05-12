package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.util.Objects;

final class MarketStreamResponseMapper {
    private MarketStreamResponseMapper() {
    }

    static MarketSummaryResponse toResponse(MarketSummaryResult result) {
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

    static MarketCandleResponse toResponse(MarketCandleResult result) {
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
