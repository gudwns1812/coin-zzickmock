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
                .getResource("db/migration/V28__add_position_peek_inventory_and_snapshots.sql");

        assertThat(initialMigration).isNotNull();
        assertThat(latestRepairMigration).isNotNull();
    }
}
