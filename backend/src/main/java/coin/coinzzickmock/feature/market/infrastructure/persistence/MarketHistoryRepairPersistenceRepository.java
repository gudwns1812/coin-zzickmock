package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairEvent;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairEventRepository;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairStatus;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MarketHistoryRepairPersistenceRepository implements MarketHistoryRepairEventRepository {
    private final MarketHistoryRepairEventEntityRepository marketHistoryRepairEventEntityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public MarketHistoryRepairEvent queueRepair(
            String symbol,
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime,
            String reason
    ) {
        LocalDateTime now = databaseDateTime(Instant.now());
        LocalDateTime databaseOpenTime = databaseDateTime(openTime);
        LocalDateTime databaseCloseTime = databaseDateTime(closeTime);
        Long eventId = upsertRepairEvent(symbol, interval, databaseOpenTime, databaseCloseTime, reason, now);
        return marketHistoryRepairEventEntityRepository.findById(eventId)
                .map(this::toEvent)
                .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketHistoryRepairEvent> findById(long eventId) {
        return marketHistoryRepairEventEntityRepository.findById(eventId).map(this::toEvent);
    }

    @Override
    @Transactional
    public boolean markProcessing(long eventId) {
        return marketHistoryRepairEventEntityRepository.markProcessing(
                eventId,
                MarketHistoryRepairStatus.QUEUED,
                MarketHistoryRepairStatus.PROCESSING,
                databaseDateTime(Instant.now())
        ) == 1;
    }

    @Override
    @Transactional
    public void markQueued(long eventId, String reason) {
        marketHistoryRepairEventEntityRepository.findById(eventId)
                .ifPresent(entity -> entity.queue(reason));
    }

    @Override
    @Transactional
    public void markWaitingForMinutes(long eventId, String reason) {
        marketHistoryRepairEventEntityRepository.markWaitingForMinutes(
                eventId,
                MarketHistoryRepairStatus.PROCESSING,
                MarketHistoryRepairStatus.WAITING_FOR_MINUTES,
                truncate(reason),
                databaseDateTime(Instant.now())
        );
    }

    @Override
    @Transactional
    public void markSucceeded(long eventId) {
        marketHistoryRepairEventEntityRepository.markSucceeded(
                eventId,
                MarketHistoryRepairStatus.PROCESSING,
                MarketHistoryRepairStatus.SUCCEEDED,
                databaseDateTime(Instant.now())
        );
    }

    @Override
    @Transactional
    public void markFailed(long eventId, String reason) {
        marketHistoryRepairEventEntityRepository.markFailed(
                eventId,
                MarketHistoryRepairStatus.PROCESSING,
                MarketHistoryRepairStatus.FAILED,
                truncate(reason),
                databaseDateTime(Instant.now())
        );
    }

    @Override
    @Transactional
    public List<Long> queueWaitingHourlyRepairEvents(String symbol, Instant hourlyOpenTime) {
        List<MarketHistoryRepairEventEntity> events =
                marketHistoryRepairEventEntityRepository.findByRepairIdentityAndStatus(
                        symbol,
                        MarketCandleInterval.ONE_HOUR,
                        databaseDateTime(hourlyOpenTime),
                        MarketHistoryRepairStatus.WAITING_FOR_MINUTES
                );
        events.forEach(event -> event.queue("minute repair completed"));
        return events.stream()
                .map(MarketHistoryRepairEventEntity::getId)
                .toList();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1024) {
            return value;
        }
        return value.substring(0, 1024);
    }

    private Long upsertRepairEvent(
            String symbol,
            MarketCandleInterval interval,
            LocalDateTime databaseOpenTime,
            LocalDateTime databaseCloseTime,
            String reason,
            LocalDateTime now
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO market_history_repair_events (
                                symbol, candle_interval, open_time, close_time, status,
                                attempt_count, last_error, created_at, updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                id = LAST_INSERT_ID(id),
                                close_time = VALUES(close_time),
                                status = CASE
                                    WHEN status IN ('PROCESSING', 'SUCCEEDED') THEN status
                                    ELSE VALUES(status)
                                END,
                                last_error = VALUES(last_error),
                                updated_at = VALUES(updated_at)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, symbol);
            statement.setString(2, interval.name());
            statement.setObject(3, databaseOpenTime);
            statement.setObject(4, databaseCloseTime);
            statement.setString(5, MarketHistoryRepairStatus.QUEUED.name());
            statement.setString(6, truncate(reason));
            statement.setObject(7, now);
            statement.setObject(8, now);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        return findRepairEventId(symbol, interval, databaseOpenTime);
    }

    private Long findRepairEventId(String symbol, MarketCandleInterval interval, LocalDateTime databaseOpenTime) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM market_history_repair_events
                        WHERE symbol = ? AND candle_interval = ? AND open_time = ?
                        """,
                Long.class,
                symbol,
                interval.name(),
                databaseOpenTime
        );
    }

    private MarketHistoryRepairEvent toEvent(MarketHistoryRepairEventEntity entity) {
        return new MarketHistoryRepairEvent(
                entity.getId(),
                entity.getSymbol(),
                entity.getInterval(),
                entity.openInstant(),
                entity.closeInstant(),
                entity.getStatus(),
                entity.getAttemptCount()
        );
    }

    private LocalDateTime databaseDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, MarketTime.STORAGE_ZONE);
    }
}
