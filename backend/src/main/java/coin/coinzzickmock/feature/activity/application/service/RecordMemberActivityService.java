package coin.coinzzickmock.feature.activity.application.service;

import coin.coinzzickmock.feature.activity.application.repository.MemberDailyActivityRepository;
import coin.coinzzickmock.feature.activity.domain.ActivityDate;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import coin.coinzzickmock.providers.Providers;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordMemberActivityService {
    private static final String RECORD_TOTAL = "dau.activity.record.total";

    private final MemberDailyActivityRepository memberDailyActivityRepository;
    private final Providers providers;
    private final Clock clock;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void record(Long memberId, ActivitySource source) {
        if (memberId == null || source == null) {
            recordMetric(source, "skipped");
            return;
        }

        Instant observedAt = Instant.now(clock);
        LocalDate activityDate = ActivityDate.from(observedAt);

        try {
            upsert(memberId, source, observedAt, activityDate);
            recordMetric(source, "success");
        } catch (RuntimeException exception) {
            recordMetric(source, "failure");
            log.warn("Failed to record member activity. source={}", source.metricValue(), exception);
        }
    }

    private void upsert(Long memberId, ActivitySource source, Instant observedAt, LocalDate activityDate) {
        memberDailyActivityRepository.findByDateAndMemberIdForUpdate(activityDate, memberId)
                .map(activity -> activity.record(observedAt, source))
                .ifPresentOrElse(
                        memberDailyActivityRepository::save,
                        () -> insertFirstActivity(activityDate, memberId, observedAt, source)
                );
    }

    private void insertFirstActivity(
            LocalDate activityDate,
            Long memberId,
            Instant observedAt,
            ActivitySource source
    ) {
        try {
            memberDailyActivityRepository.save(MemberDailyActivity.firstSeen(
                    activityDate,
                    memberId,
                    observedAt,
                    source
            ));
        } catch (DataIntegrityViolationException exception) {
            memberDailyActivityRepository.findByDateAndMemberIdForUpdate(activityDate, memberId)
                    .map(activity -> activity.record(observedAt, source))
                    .map(memberDailyActivityRepository::save)
                    .orElseThrow(() -> exception);
        }
    }

    private void recordMetric(ActivitySource source, String result) {
        providers.telemetry().recordEvent(RECORD_TOTAL, Map.of(
                "source", source == null ? "unknown" : source.metricValue(),
                "result", result
        ));
    }
}
