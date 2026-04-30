package coin.coinzzickmock.feature.account.api;

public record AccountSummaryResponse(
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
