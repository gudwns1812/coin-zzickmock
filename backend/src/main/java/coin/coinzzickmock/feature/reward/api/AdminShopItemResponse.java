package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.feature.reward.application.result.AdminShopItemResult;

public record AdminShopItemResponse(
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
    public static AdminShopItemResponse from(AdminShopItemResult result) {
        return new AdminShopItemResponse(
                result.code(),
                result.name(),
                result.description(),
                result.itemType(),
                result.price(),
                result.active(),
                result.totalStock(),
                result.soldQuantity(),
                result.remainingStock(),
                result.perMemberPurchaseLimit(),
                result.sortOrder()
        );
    }
}
