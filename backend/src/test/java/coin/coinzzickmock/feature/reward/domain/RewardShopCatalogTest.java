package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardShopCatalogTest {
    @Test
    void exposesDefaultRewardShopItems() {
        assertEquals(3, RewardShopCatalog.defaultItems().size());
        assertEquals("badge.basic", RewardShopCatalog.defaultItems().get(0).code());
        assertEquals("프로필 배지", RewardShopCatalog.defaultItems().get(0).name());
    }
}
