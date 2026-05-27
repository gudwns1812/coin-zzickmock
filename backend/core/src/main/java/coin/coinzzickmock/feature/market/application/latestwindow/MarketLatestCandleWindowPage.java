package coin.coinzzickmock.feature.market.application.latestwindow;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
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
