package coin.coinzzickmock.feature.activity.application.repository;

import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import java.time.LocalDate;

public interface MemberDailyActivityRepository {
    void record(MemberDailyActivity activity);

    long countByDate(LocalDate activityDate);
}
