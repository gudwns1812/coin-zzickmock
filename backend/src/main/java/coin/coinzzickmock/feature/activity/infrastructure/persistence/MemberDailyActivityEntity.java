package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "member_daily_activity")
public class MemberDailyActivityEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "activity_count", nullable = false)
    private long activityCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_source", nullable = false, length = 40)
    private ActivitySource firstSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_source", nullable = false, length = 40)
    private ActivitySource lastSource;

    protected MemberDailyActivityEntity() {
    }

    public MemberDailyActivityEntity(
            Long id,
            LocalDate activityDate,
            Long memberId,
            Instant firstSeenAt,
            Instant lastSeenAt,
            long activityCount,
            ActivitySource firstSource,
            ActivitySource lastSource
    ) {
        this.id = id;
        this.activityDate = activityDate;
        this.memberId = memberId;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.activityCount = activityCount;
        this.firstSource = firstSource;
        this.lastSource = lastSource;
    }

    public static MemberDailyActivityEntity from(MemberDailyActivity activity) {
        return new MemberDailyActivityEntity(
                null,
                activity.activityDate(),
                activity.memberId(),
                activity.firstSeenAt(),
                activity.lastSeenAt(),
                activity.activityCount(),
                activity.firstSource(),
                activity.lastSource()
        );
    }

    public void apply(MemberDailyActivity activity) {
        this.firstSeenAt = activity.firstSeenAt();
        this.lastSeenAt = activity.lastSeenAt();
        this.activityCount = activity.activityCount();
        this.firstSource = activity.firstSource();
        this.lastSource = activity.lastSource();
    }

    public MemberDailyActivity toDomain() {
        return new MemberDailyActivity(
                activityDate,
                memberId,
                firstSeenAt,
                lastSeenAt,
                activityCount,
                firstSource,
                lastSource
        );
    }
}
