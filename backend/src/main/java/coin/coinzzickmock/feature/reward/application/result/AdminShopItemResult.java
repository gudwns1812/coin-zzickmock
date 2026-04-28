package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardShopItem;

public record AdminShopItemResult(
        String code,
        String name,
        String description,
        String itemType,
        int price,
        boolean active,
        Integer totalStock,
        int soldQuantity,
        Integer remainingStock,
        Integer perMemberPurchaseLimit,
        int sortOrder
) {
    public static AdminShopItemResult from(RewardShopItem item) {
        return new AdminShopItemResult(
                item.code(),
                item.name(),
                item.description(),
                item.itemType(),
                item.price(),
                item.active(),
                item.totalStock(),
                item.soldQuantity(),
                item.remainingStock(),
                item.perMemberPurchaseLimit(),
                item.sortOrder()
        );
    }
}
