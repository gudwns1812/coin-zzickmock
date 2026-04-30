package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardShopItemTest {
    @Test
    void reportsFiniteStockAndSoldOutStateFromSoldQuantity() {
        RewardShopItem item = new RewardShopItem(
                1L,
                "voucher.coffee",
                "커피 교환권",
                "커피 교환권",
                "COFFEE_VOUCHER",
                100,
                true,
                3,
                3,
                1,
                10
        );

        assertTrue(item.soldOut());
        assertEquals(0, item.remainingStock());
        assertTrue(item.memberReachedLimit(1));
        assertEquals(0, item.remainingPurchaseLimit(1));
    }

    @Test
    void leavesUnlimitedStockAndPurchaseLimitAsNull() {
        RewardShopItem item = new RewardShopItem("badge.basic", "프로필 배지", 10, "기본 배지");

        assertFalse(item.finiteStock());
        assertFalse(item.soldOut());
        assertNull(item.remainingStock());
        assertFalse(item.hasPurchaseLimit());
        assertNull(item.remainingPurchaseLimit(100));
    }

    @Test
    void rejectsInvalidInventoryAndPurchaseLimitCounters() {
        assertThrows(IllegalArgumentException.class, () -> new RewardShopItem(
                1L,
                "voucher.coffee",
                "커피 교환권",
                "커피 교환권",
                "COFFEE_VOUCHER",
                100,
                true,
                3,
                4,
                1,
                10
        ));
        assertThrows(IllegalArgumentException.class, () -> new RewardShopItem(
                1L,
                "voucher.coffee",
                "커피 교환권",
                "커피 교환권",
                "COFFEE_VOUCHER",
                100,
                true,
                3,
                0,
                0,
                10
        ));
    }
}
