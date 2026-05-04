package coin.coinzzickmock.feature.reward.application.result;

public record ShopPurchaseResult(
        String itemCode,
        int rewardPoint,
        int refillRemainingCount
) {
}
