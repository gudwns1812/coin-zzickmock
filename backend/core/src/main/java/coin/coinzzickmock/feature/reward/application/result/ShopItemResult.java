package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardShopItem;

public record ShopItemResult(
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
        Integer remainingPurchaseLimit
) {
    public static ShopItemResult from(RewardShopItem item, int purchaseCount) {
        return new ShopItemResult(
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
                item.remainingPurchaseLimit(purchaseCount)
        );
    }
}
