package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Transactional
class MemberDailyActivityPersistenceRepositoryTest {
    @Autowired
    private MemberDailyActivityPersistenceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recordsSameMemberAndDateIdempotentlyWithNativeUpsert() {
        Long memberId = demoMemberId();
        LocalDate activityDate = LocalDate.of(2026, 5, 2);
        Instant first = Instant.parse("2026-05-02T01:00:00Z");
        Instant second = Instant.parse("2026-05-02T02:00:00Z");

        repository.record(MemberDailyActivity.firstSeen(activityDate, memberId, first, ActivitySource.LOGIN));
        repository.record(MemberDailyActivity.firstSeen(activityDate, memberId, second, ActivitySource.AUTHENTICATED_API));

        assertThat(rowCount(memberId, activityDate)).isEqualTo(1);
        assertThat(activityCount(memberId, activityDate)).isEqualTo(2);
        assertThat(sourceColumn(memberId, activityDate, "first_source")).isEqualTo("LOGIN");
        assertThat(sourceColumn(memberId, activityDate, "last_source")).isEqualTo("AUTHENTICATED_API");
        assertThat(instantColumn(memberId, activityDate, "first_seen_at")).isEqualTo(first);
        assertThat(instantColumn(memberId, activityDate, "last_seen_at")).isEqualTo(second);
    }

    @Test
    void olderAsyncEventDoesNotRegressLastSeenOrLastSource() {
        Long memberId = demoMemberId();
        LocalDate activityDate = LocalDate.of(2026, 5, 3);
        Instant later = Instant.parse("2026-05-03T05:00:00Z");
        Instant earlier = Instant.parse("2026-05-03T03:00:00Z");

        repository.record(MemberDailyActivity.firstSeen(activityDate, memberId, later, ActivitySource.AUTHENTICATED_API));
        repository.record(MemberDailyActivity.firstSeen(activityDate, memberId, earlier, ActivitySource.LOGIN));

        assertThat(rowCount(memberId, activityDate)).isEqualTo(1);
        assertThat(activityCount(memberId, activityDate)).isEqualTo(2);
        assertThat(sourceColumn(memberId, activityDate, "first_source")).isEqualTo("LOGIN");
        assertThat(sourceColumn(memberId, activityDate, "last_source")).isEqualTo("AUTHENTICATED_API");
        assertThat(instantColumn(memberId, activityDate, "first_seen_at")).isEqualTo(earlier);
        assertThat(instantColumn(memberId, activityDate, "last_seen_at")).isEqualTo(later);
    }

    private Long demoMemberId() {
        return jdbcTemplate.queryForObject(
                "select id from member_credentials where account = 'test'",
                Long.class
        );
    }

    private long rowCount(Long memberId, LocalDate activityDate) {
        return jdbcTemplate.queryForObject(
                "select count(*) from member_daily_activity where member_id = ? and activity_date = ?",
                Long.class,
                memberId,
                activityDate
        );
    }

    private long activityCount(Long memberId, LocalDate activityDate) {
        return jdbcTemplate.queryForObject(
                "select activity_count from member_daily_activity where member_id = ? and activity_date = ?",
                Long.class,
                memberId,
                activityDate
        );
    }

    private String sourceColumn(Long memberId, LocalDate activityDate, String column) {
        return jdbcTemplate.queryForObject(
                "select " + column + " from member_daily_activity where member_id = ? and activity_date = ?",
                String.class,
                memberId,
                activityDate
        );
    }

    private Instant instantColumn(Long memberId, LocalDate activityDate, String column) {
        return jdbcTemplate.queryForObject(
                "select " + column + " from member_daily_activity where member_id = ? and activity_date = ?",
                (rs, rowNum) -> rs.getObject(column, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                memberId,
                activityDate
        );
    }
}
