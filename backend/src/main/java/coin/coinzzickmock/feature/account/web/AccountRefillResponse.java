package coin.coinzzickmock.feature.account.web;

public record AccountRefillResponse(
        double walletBalance,
        double availableMargin,
        int remainingCount
) {
}
