package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LeaderboardWalletBalanceChangedListener {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWalletBalanceChanged(WalletBalanceChangedEvent event) {
        refreshLeaderboardService.refreshMember(event.memberId());
    }
}
