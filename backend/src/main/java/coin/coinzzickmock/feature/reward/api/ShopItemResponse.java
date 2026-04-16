package coin.coinzzickmock.feature.reward.api;

public record ShopItemResponse(
        String code,
        String name,
        double price,
        String description
) {
}
