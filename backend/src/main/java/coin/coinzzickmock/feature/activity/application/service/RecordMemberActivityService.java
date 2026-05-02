package coin.coinzzickmock.feature.activity.application.service;

import coin.coinzzickmock.feature.activity.application.event.MemberActivityObservedEvent;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.providers.Providers;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordMemberActivityService {
    private static final String RECORD_TOTAL = "dau.activity.record.total";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Providers providers;
    private final Clock clock;

    public void record(Long memberId, ActivitySource source) {
        if (memberId == null || source == null) {
            recordMetric(source, "skipped");
            return;
        }

        Instant observedAt = Instant.now(clock);
        try {
            applicationEventPublisher.publishEvent(new MemberActivityObservedEvent(memberId, source, observedAt));
            recordMetric(source, "queued");
        } catch (TaskRejectedException exception) {
            recordMetric(source, "rejected");
            log.warn("Rejected member activity collection. source={}", source.metricValue(), exception);
        }
    }

    private void recordMetric(ActivitySource source, String result) {
        providers.telemetry().recordEvent(RECORD_TOTAL, Map.of(
                "source", source == null ? "unknown" : source.metricValue(),
                "result", result
        ));
    }
}
