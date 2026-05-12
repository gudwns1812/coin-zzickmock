package coin.coinzzickmock.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import org.junit.jupiter.api.Test;

class StorageMigrationResourceTest {
    @Test
    void exposesFlywayMigrationsFromStorageClasspath() {
        URL initialMigration = Thread.currentThread()
                .getContextClassLoader()
                .getResource("db/migration/V1__initial_schema.sql");
        URL latestRepairMigration = Thread.currentThread()
                .getContextClassLoader()
                .getResource("db/migration/V26__add_market_history_repair_events.sql");

        assertThat(initialMigration).isNotNull();
        assertThat(latestRepairMigration).isNotNull();
    }
}
