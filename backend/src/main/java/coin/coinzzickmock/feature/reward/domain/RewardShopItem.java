package coin.coinzzickmock.feature.reward.domain;

public record RewardShopItem(
        String code,
        String name,
        double price,
        String description
) {
}
