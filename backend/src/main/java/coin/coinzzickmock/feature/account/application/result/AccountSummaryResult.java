package coin.coinzzickmock.feature.account.application.result;

public record AccountSummaryResult(
        Long memberId,
        String account,
        String memberName,
        String nickname,
        double usdtBalance,
        double walletBalance,
        double available,
        double totalUnrealizedPnl,
        double roi,
        int rewardPoint
) {
}
