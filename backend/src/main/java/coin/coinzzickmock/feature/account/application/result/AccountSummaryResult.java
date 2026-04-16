package coin.coinzzickmock.feature.account.application.result;

public record AccountSummaryResult(
        String memberId,
        String memberName,
        double walletBalance,
        double availableMargin,
        double rewardPoint
) {
}
