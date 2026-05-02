package coin.coinzzickmock.feature.leaderboard.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaderboardEntry(
        Long memberId,
        String nickname,
        double walletBalance,
        double profitRate,
        Instant updatedAt
) {
    private static final double INITIAL_WALLET_BALANCE = 100_000;

    public static LeaderboardEntry fromWalletBalance(
            Long memberId,
            String nickname,
            double walletBalance,
            Instant updatedAt
    ) {
        return fromWalletBalance(memberId, nickname, BigDecimal.valueOf(walletBalance), updatedAt);
    }

    public static LeaderboardEntry fromWalletBalance(
            Long memberId,
            String nickname,
            BigDecimal walletBalance,
            Instant updatedAt
    ) {
        double walletBalanceValue = walletBalance == null ? 0 : walletBalance.doubleValue();
        return new LeaderboardEntry(
                memberId,
                nickname,
                walletBalanceValue,
                (walletBalanceValue - INITIAL_WALLET_BALANCE) / INITIAL_WALLET_BALANCE,
                updatedAt
        );
    }
}
