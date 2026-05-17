package coin.coinzzickmock.feature.market.application.dto;

import java.time.Instant;

public record MarketMinuteClosedEvent(Instant openTime, Instant closeTime) {
}
