package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.math.BigDecimal;
import java.util.Objects;

final class MarketHttpResponseMapper {
    private MarketHttpResponseMapper() {
    }

    static MarketSummaryHttpResponse toResponse(MarketSummaryResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new MarketSummaryHttpResponse(
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

    static MarketCandleHttpResponse toResponse(MarketCandleResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new MarketCandleHttpResponse(
                result.openTime(),
                result.closeTime(),
                BigDecimal.valueOf(result.openPrice()),
                BigDecimal.valueOf(result.highPrice()),
                BigDecimal.valueOf(result.lowPrice()),
                BigDecimal.valueOf(result.closePrice()),
                BigDecimal.valueOf(result.volume())
        );
    }
}
