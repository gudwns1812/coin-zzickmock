package coin.coinzzickmock.feature.reward.web;

public record ShopPurchaseResponse(
        String itemCode,
        int rewardPoint,
        int refillRemainingCount
) {
}
