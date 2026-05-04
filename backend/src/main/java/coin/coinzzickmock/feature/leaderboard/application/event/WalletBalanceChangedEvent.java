package coin.coinzzickmock.feature.leaderboard.application.event;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.time.Instant;

public record WalletBalanceChangedEvent(
        Long memberId,
        Double walletBalance,
        Long accountVersion,
        Instant observedAt
) {
    public WalletBalanceChangedEvent(Long memberId) {
        this(memberId, null, null, Instant.now());
    }

    public static WalletBalanceChangedEvent from(TradingAccount account) {
        return new WalletBalanceChangedEvent(
                account.memberId(),
                account.walletBalance(),
                account.version(),
                Instant.now()
        );
    }

    public boolean hasAccountSnapshot() {
        return walletBalance != null && accountVersion != null;
    }
}
