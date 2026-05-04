package coin.coinzzickmock.feature.account.application.result;

public record AccountRefillResult(
        double walletBalance,
        double availableMargin,
        int remainingCount
) {
}
