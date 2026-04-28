package coin.coinzzickmock.feature.reward.application.result;

public record ShopItemResult(
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
