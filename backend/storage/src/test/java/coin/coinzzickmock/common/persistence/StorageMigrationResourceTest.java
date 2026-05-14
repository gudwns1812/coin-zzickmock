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
        URL restoredCommunityMigration = Thread.currentThread()
                .getContextClassLoader()
                .getResource("db/migration/V31__add_community_posts.sql");
        URL latestRewardShopPurchaseMigration = Thread.currentThread()
                .getContextClassLoader()
                .getResource("db/migration/V32__add_reward_shop_purchases.sql");

        assertThat(initialMigration).isNotNull();
        assertThat(restoredCommunityMigration).isNotNull();
        assertThat(latestRewardShopPurchaseMigration).isNotNull();
    }
}
