package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.feature.activity.application.repository.MemberDailyActivityRepository;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDailyActivityPersistenceRepository implements MemberDailyActivityRepository {
    private static final DateTimeFormatter UTC_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final MemberDailyActivityEntityRepository entityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void record(MemberDailyActivity activity) {
        String observedAt = utcDateTimeText(activity.lastSeenAt());
        String now = utcDateTimeText(Instant.now());
        jdbcTemplate.update(
                """
                        INSERT INTO member_daily_activity (
                            activity_date, member_id, first_seen_at, last_seen_at, activity_count,
                            first_source, last_source, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            first_source = CASE
                                WHEN VALUES(first_seen_at) <= first_seen_at THEN VALUES(first_source)
                                ELSE first_source
                            END,
                            last_source = CASE
                                WHEN VALUES(last_seen_at) >= last_seen_at THEN VALUES(last_source)
                                ELSE last_source
                            END,
                            first_seen_at = LEAST(first_seen_at, VALUES(first_seen_at)),
                            last_seen_at = GREATEST(last_seen_at, VALUES(last_seen_at)),
                            activity_count = activity_count + VALUES(activity_count),
                            updated_at = VALUES(updated_at)
                        """,
                activity.activityDate(),
                activity.memberId(),
                observedAt,
                observedAt,
                activity.activityCount(),
                activity.firstSource().name(),
                activity.lastSource().name(),
                now,
                now
        );
    }

    @Override
    public long countByDate(LocalDate activityDate) {
        return entityRepository.countByActivityDate(activityDate);
    }

    private static String utcDateTimeText(Instant instant) {
        return UTC_DATETIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
