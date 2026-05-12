package coin.coinzzickmock.feature.activity.job;

import coin.coinzzickmock.feature.activity.application.service.SnapshotDailyActiveUserSummaryService;
import coin.coinzzickmock.feature.activity.domain.ActivityDate;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "coin.activity.summary",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DailyActiveUserSummaryScheduler {
    private final SnapshotDailyActiveUserSummaryService snapshotDailyActiveUserSummaryService;

    @Scheduled(cron = "${coin.activity.summary.cron:0 5 0 * * *}", zone = "Asia/Seoul")
    public void snapshotYesterday() {
        snapshotDailyActiveUserSummaryService.snapshot(LocalDate.now(ActivityDate.REPORTING_ZONE).minusDays(1));
    }
}
