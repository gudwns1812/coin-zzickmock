package coin.coinzzickmock.feature.account.api;

public record AccountSummaryResponse(
        String memberId,
        String memberName,
        double walletBalance,
        double availableMargin,
        double rewardPoint
) {
}
