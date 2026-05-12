package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairStatus;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketHistoryRepairEventEntityRepository extends JpaRepository<MarketHistoryRepairEventEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MarketHistoryRepairEventEntity event
            SET event.status = :processingStatus,
                event.attemptCount = event.attemptCount + 1,
                event.updatedAt = :updatedAt
            WHERE event.id = :eventId AND event.status = :queuedStatus
            """)
    int markProcessing(
            @Param("eventId") long eventId,
            @Param("queuedStatus") MarketHistoryRepairStatus queuedStatus,
            @Param("processingStatus") MarketHistoryRepairStatus processingStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MarketHistoryRepairEventEntity event
            SET event.status = :waitingStatus,
                event.lastError = :reason,
                event.updatedAt = :updatedAt
            WHERE event.id = :eventId AND event.status = :processingStatus
            """)
    int markWaitingForMinutes(
            @Param("eventId") long eventId,
            @Param("processingStatus") MarketHistoryRepairStatus processingStatus,
            @Param("waitingStatus") MarketHistoryRepairStatus waitingStatus,
            @Param("reason") String reason,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MarketHistoryRepairEventEntity event
            SET event.status = :succeededStatus,
                event.lastError = NULL,
                event.updatedAt = :updatedAt
            WHERE event.id = :eventId AND event.status = :processingStatus
            """)
    int markSucceeded(
            @Param("eventId") long eventId,
            @Param("processingStatus") MarketHistoryRepairStatus processingStatus,
            @Param("succeededStatus") MarketHistoryRepairStatus succeededStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MarketHistoryRepairEventEntity event
            SET event.status = :failedStatus,
                event.lastError = :reason,
                event.updatedAt = :updatedAt
            WHERE event.id = :eventId AND event.status = :processingStatus
            """)
    int markFailed(
            @Param("eventId") long eventId,
            @Param("processingStatus") MarketHistoryRepairStatus processingStatus,
            @Param("failedStatus") MarketHistoryRepairStatus failedStatus,
            @Param("reason") String reason,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query("""
            SELECT event
            FROM MarketHistoryRepairEventEntity event
            WHERE event.symbol = :symbol
              AND event.interval = :interval
              AND event.openTime = :openTime
              AND event.status = :status
            """)
    List<MarketHistoryRepairEventEntity> findByRepairIdentityAndStatus(
            @Param("symbol") String symbol,
            @Param("interval") MarketCandleInterval interval,
            @Param("openTime") LocalDateTime openTime,
            @Param("status") MarketHistoryRepairStatus status
    );
}
