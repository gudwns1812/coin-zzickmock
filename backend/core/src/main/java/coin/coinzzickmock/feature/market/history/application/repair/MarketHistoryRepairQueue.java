package coin.coinzzickmock.feature.market.history.application.repair;

import java.time.Duration;
import java.util.Optional;

public interface MarketHistoryRepairQueue {
    void push(long eventId);

    Optional<Long> pop(Duration timeout);
}
