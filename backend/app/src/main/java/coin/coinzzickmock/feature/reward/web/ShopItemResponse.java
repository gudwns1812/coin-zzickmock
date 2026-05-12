package coin.coinzzickmock.feature.reward.web;

public record ShopItemResponse(
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
}
