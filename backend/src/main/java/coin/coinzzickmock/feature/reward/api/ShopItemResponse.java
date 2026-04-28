package coin.coinzzickmock.feature.reward.api;

public record ShopItemResponse(
        String code,
        String name,
        String description,
        int price,
        boolean active,
        Integer totalStock,
        int soldQuantity,
        Integer remainingStock,
        Integer perMemberPurchaseLimit,
        Integer remainingPurchaseLimit
) {
}
