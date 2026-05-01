package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "member_daily_activity")
public class MemberDailyActivityEntity extends AuditableEntity {
    @EmbeddedId
    private MemberDailyActivityId id;

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
            MemberDailyActivityId id,
            Instant firstSeenAt,
            Instant lastSeenAt,
            long activityCount,
            ActivitySource firstSource,
            ActivitySource lastSource
    ) {
        this.id = id;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.activityCount = activityCount;
        this.firstSource = firstSource;
        this.lastSource = lastSource;
    }

    public static MemberDailyActivityEntity from(MemberDailyActivity activity) {
        return new MemberDailyActivityEntity(
                new MemberDailyActivityId(activity.activityDate(), activity.memberId()),
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
                id.activityDate(),
                id.memberId(),
                firstSeenAt,
                lastSeenAt,
                activityCount,
                firstSource,
                lastSource
        );
    }
}
