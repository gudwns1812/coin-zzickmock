package coin.coinzzickmock.feature.activity.application.repository;

import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import java.time.LocalDate;
import java.util.Optional;

public interface MemberDailyActivityRepository {
    Optional<MemberDailyActivity> findByDateAndMemberIdForUpdate(LocalDate activityDate, Long memberId);

    MemberDailyActivity save(MemberDailyActivity activity);

    long countByDate(LocalDate activityDate);
}
