package coin.coinzzickmock.feature.market.latestwindow.application.dto;

import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.List;

public record MarketLatestCandleWindowPage(
        List<MarketCandleResult> candles,
        MarketCandleInterval interval,
        int limit,
        Instant latestOutputOpenTime,
        Instant generatedAt
) {
}
