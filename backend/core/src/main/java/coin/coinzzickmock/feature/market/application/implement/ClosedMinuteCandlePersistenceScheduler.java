package coin.coinzzickmock.feature.market.application.implement;

import java.time.Instant;
import java.util.List;

public interface ClosedMinuteCandlePersistenceScheduler {
    void scheduleClosedMinutePersistence(List<String> symbols, Instant openTime, Instant closeTime);
}
