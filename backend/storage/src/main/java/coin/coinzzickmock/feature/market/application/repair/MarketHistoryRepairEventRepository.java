package coin.coinzzickmock.feature.market.application.repair;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketHistoryRepairEventRepository {
    MarketHistoryRepairEvent queueRepair(
            String symbol,
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime,
            String reason
    );

    Optional<MarketHistoryRepairEvent> findById(long eventId);

    boolean markProcessing(long eventId);

    void markQueued(long eventId, String reason);

    void markWaitingForMinutes(long eventId, String reason);

    void markSucceeded(long eventId);

    void markFailed(long eventId, String reason);

    List<Long> queueWaitingHourlyRepairEvents(String symbol, Instant hourlyOpenTime);
}
