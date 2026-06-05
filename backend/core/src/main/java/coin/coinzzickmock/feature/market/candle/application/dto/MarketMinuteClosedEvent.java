package coin.coinzzickmock.feature.market.candle.application.dto;

import java.time.Instant;

public record MarketMinuteClosedEvent(Instant openTime, Instant closeTime) {
}
