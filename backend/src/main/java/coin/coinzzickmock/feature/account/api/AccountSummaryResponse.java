package coin.coinzzickmock.feature.account.api;

public record AccountSummaryResponse(
        String memberId,
        String memberName,
        double usdtBalance,
        double walletBalance,
        double available,
        double totalUnrealizedPnl,
        double roi,
        double rewardPoint
) {
}
