package coin.coinzzickmock.feature.account.application.result;

public record AccountSummaryResult(
        String memberId,
        String memberName,
        double usdtBalance,
        double walletBalance,
        double available,
        double totalUnrealizedPnl,
        double roi,
        int rewardPoint
) {
}
