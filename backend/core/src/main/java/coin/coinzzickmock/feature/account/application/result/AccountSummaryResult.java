package coin.coinzzickmock.feature.account.application.result;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import java.util.List;

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
    public static AccountSummaryResult from(
            TradingAccount account,
            MemberCredential member,
            RewardPointWallet rewardPointWallet,
            List<PositionSnapshot> positions
    ) {
        double totalUnrealizedPnl = positions.stream()
                .mapToDouble(PositionSnapshot::unrealizedPnl)
                .sum();
        double totalOpenInitialMargin = positions.stream()
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
        double roi = totalOpenInitialMargin == 0 ? 0 : totalUnrealizedPnl / totalOpenInitialMargin;

        return new AccountSummaryResult(
                account.memberId(),
                member.account(),
                account.memberName(),
                member.nickname(),
                account.walletBalance() + totalUnrealizedPnl,
                account.walletBalance(),
                account.availableMargin(),
                totalUnrealizedPnl,
                roi,
                rewardPointWallet.rewardPoint()
        );
    }
}
