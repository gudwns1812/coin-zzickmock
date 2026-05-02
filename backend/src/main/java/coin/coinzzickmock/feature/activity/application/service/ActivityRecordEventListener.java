package coin.coinzzickmock.feature.activity.application.service;

import coin.coinzzickmock.feature.activity.application.event.MemberActivityObservedEvent;
import coin.coinzzickmock.feature.activity.application.repository.MemberDailyActivityRepository;
import coin.coinzzickmock.feature.activity.domain.ActivityDate;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import coin.coinzzickmock.providers.Providers;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityRecordEventListener {
    private static final String RECORD_TOTAL = "dau.activity.record.total";

    private final MemberDailyActivityRepository memberDailyActivityRepository;
    private final Providers providers;

    @Async("activityRecordExecutor")
    @EventListener
    public void on(MemberActivityObservedEvent event) {
        try {
            memberDailyActivityRepository.record(MemberDailyActivity.firstSeen(
                    ActivityDate.from(event.observedAt()),
                    event.memberId(),
                    event.observedAt(),
                    event.source()
            ));
            recordMetric(event.source(), "success");
        } catch (RuntimeException exception) {
            recordMetric(event.source(), "failure");
            log.warn("Failed to record member activity. source={}", event.source().metricValue(), exception);
        }
    }

    private void recordMetric(ActivitySource source, String result) {
        providers.telemetry().recordEvent(RECORD_TOTAL, Map.of(
                "source", source == null ? "unknown" : source.metricValue(),
                "result", result
        ));
    }
}
