package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairStatus;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_history_repair_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketHistoryRepairEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "candle_interval", nullable = false, length = 16)
    private MarketCandleInterval interval;

    @Column(name = "open_time", nullable = false)
    private LocalDateTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalDateTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MarketHistoryRepairStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MarketHistoryRepairEventEntity(
            String symbol,
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime,
            String reason
    ) {
        LocalDateTime now = now();
        this.symbol = symbol;
        this.interval = interval;
        this.openTime = databaseDateTime(openTime);
        this.closeTime = databaseDateTime(closeTime);
        this.status = MarketHistoryRepairStatus.QUEUED;
        this.attemptCount = 0;
        this.lastError = truncate(reason);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void queue(String reason) {
        if (status == MarketHistoryRepairStatus.SUCCEEDED) {
            return;
        }
        status = MarketHistoryRepairStatus.QUEUED;
        lastError = truncate(reason);
        updatedAt = now();
    }

    public boolean startProcessing() {
        if (status != MarketHistoryRepairStatus.QUEUED) {
            return false;
        }
        status = MarketHistoryRepairStatus.PROCESSING;
        attemptCount++;
        updatedAt = now();
        return true;
    }

    public void waitingForMinutes(String reason) {
        status = MarketHistoryRepairStatus.WAITING_FOR_MINUTES;
        lastError = truncate(reason);
        updatedAt = now();
    }

    public void succeeded() {
        status = MarketHistoryRepairStatus.SUCCEEDED;
        lastError = null;
        updatedAt = now();
    }

    public void failed(String reason) {
        status = MarketHistoryRepairStatus.FAILED;
        lastError = truncate(reason);
        updatedAt = now();
    }

    public Instant openInstant() {
        return openTime.toInstant(MarketTime.STORAGE_ZONE);
    }

    public Instant closeInstant() {
        return closeTime.toInstant(MarketTime.STORAGE_ZONE);
    }

    private LocalDateTime now() {
        return databaseDateTime(Instant.now());
    }

    private LocalDateTime databaseDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, MarketTime.STORAGE_ZONE);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1024) {
            return value;
        }
        return value.substring(0, 1024);
    }
}
