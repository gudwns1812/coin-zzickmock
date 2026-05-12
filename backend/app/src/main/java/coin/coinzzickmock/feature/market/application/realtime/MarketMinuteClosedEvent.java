package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Instant;

public record MarketMinuteClosedEvent(Instant openTime, Instant closeTime) {
}
