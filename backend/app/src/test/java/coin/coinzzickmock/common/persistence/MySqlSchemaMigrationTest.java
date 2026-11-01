package coin.coinzzickmock.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.testsupport.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 컨텍스트가 뜨면 Flyway 마이그레이션 전체와 Hibernate ddl-auto=validate가 MySQL 8.4에서 통과한 것이다.
 */
@MySqlIntegrationTest
@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class MySqlSchemaMigrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesEveryMigrationOnProductionMySqlVersion() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL",
                Integer.class
        );
        Integer failedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0",
                Integer.class
        );

        assertThat(version).startsWith("8.4");
        assertThat(appliedMigrations).isPositive();
        assertThat(failedMigrations).isZero();
    }

    @Test
    void usesRepeatableReadLikeProductionInnoDb() {
        String isolation = jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class);
        String lockWaitTimeout = jdbcTemplate.queryForObject("SELECT @@innodb_lock_wait_timeout", String.class);

        assertThat(isolation).isEqualTo("REPEATABLE-READ");
        assertThat(lockWaitTimeout).isEqualTo("5");
    }
}
