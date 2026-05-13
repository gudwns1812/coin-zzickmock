package coin.coinzzickmock.feature.reward.application.result;

public record PositionPeekItemBalanceResult(
        String itemCode,
        int remainingQuantity
) {
}
