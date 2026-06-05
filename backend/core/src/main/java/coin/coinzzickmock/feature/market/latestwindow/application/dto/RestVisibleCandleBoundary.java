package coin.coinzzickmock.feature.market.latestwindow.application.dto;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record RestVisibleCandleBoundary(
        long symbolId,
        MarketCandleInterval interval,
        Instant latestOutputOpenTime
) {
}
