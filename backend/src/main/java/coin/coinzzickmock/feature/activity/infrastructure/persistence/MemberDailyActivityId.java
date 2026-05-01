package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class MemberDailyActivityId implements Serializable {
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    protected MemberDailyActivityId() {
    }

    public MemberDailyActivityId(LocalDate activityDate, Long memberId) {
        this.activityDate = activityDate;
        this.memberId = memberId;
    }

    public LocalDate activityDate() {
        return activityDate;
    }

    public Long memberId() {
        return memberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberDailyActivityId that)) {
            return false;
        }
        return Objects.equals(activityDate, that.activityDate) && Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activityDate, memberId);
    }
}
