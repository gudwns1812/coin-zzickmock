package coin.coinzzickmock.feature.market.application.latestwindow;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record RestVisibleCandleBoundary(
        long symbolId,
        MarketCandleInterval interval,
        Instant latestOutputOpenTime
) {
}
