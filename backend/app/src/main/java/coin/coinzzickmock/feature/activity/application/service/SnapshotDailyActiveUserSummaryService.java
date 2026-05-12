package coin.coinzzickmock.feature.activity.application.service;

import coin.coinzzickmock.feature.activity.application.repository.DailyActiveUserSummaryRepository;
import coin.coinzzickmock.feature.activity.application.repository.MemberDailyActivityRepository;
import coin.coinzzickmock.feature.activity.domain.DailyActiveUserSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SnapshotDailyActiveUserSummaryService {
    private final MemberDailyActivityRepository memberDailyActivityRepository;
    private final DailyActiveUserSummaryRepository dailyActiveUserSummaryRepository;
    private final Clock clock;

    @Transactional
    public DailyActiveUserSummary snapshot(LocalDate activityDate) {
        long count = memberDailyActivityRepository.countByDate(activityDate);
        return dailyActiveUserSummaryRepository.save(new DailyActiveUserSummary(
                activityDate,
                count,
                Instant.now(clock)
        ));
    }
}
