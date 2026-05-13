package coin.coinzzickmock.feature.reward.application.result;

public record ShopPurchaseResult(
        String itemCode,
        int rewardPoint,
        Integer refillRemainingCount,
        Integer positionPeekItemBalance
) {
    public static ShopPurchaseResult accountRefill(
            String itemCode,
            int rewardPoint,
            int refillRemainingCount
    ) {
        return new ShopPurchaseResult(itemCode, rewardPoint, refillRemainingCount, null);
    }

    public static ShopPurchaseResult positionPeek(
            String itemCode,
            int rewardPoint,
            int positionPeekItemBalance
    ) {
        return new ShopPurchaseResult(itemCode, rewardPoint, null, positionPeekItemBalance);
    }
}
