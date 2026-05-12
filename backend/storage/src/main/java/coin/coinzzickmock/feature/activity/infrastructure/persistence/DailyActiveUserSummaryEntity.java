package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.activity.domain.DailyActiveUserSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_active_user_summary")
public class DailyActiveUserSummaryEntity extends AuditableEntity {
    @Id
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "active_user_count", nullable = false)
    private long activeUserCount;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;

    protected DailyActiveUserSummaryEntity() {
    }

    public DailyActiveUserSummaryEntity(LocalDate activityDate, long activeUserCount, Instant sampledAt) {
        this.activityDate = activityDate;
        this.activeUserCount = activeUserCount;
        this.sampledAt = sampledAt;
    }

    public static DailyActiveUserSummaryEntity from(DailyActiveUserSummary summary) {
        return new DailyActiveUserSummaryEntity(
                summary.activityDate(),
                summary.activeUserCount(),
                summary.sampledAt()
        );
    }

    public void apply(DailyActiveUserSummary summary) {
        this.activeUserCount = summary.activeUserCount();
        this.sampledAt = summary.sampledAt();
    }

    public DailyActiveUserSummary toDomain() {
        return new DailyActiveUserSummary(activityDate, activeUserCount, sampledAt);
    }
}
