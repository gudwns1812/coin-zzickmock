package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletHistoryBalanceChangedListener {
    private final SnapshotWalletHistoryService snapshotWalletHistoryService;

    @EventListener
    public void onWalletBalanceChanged(WalletBalanceChangedEvent event) {
        snapshotWalletHistoryService.recordChangedBalance(event);
    }
}
