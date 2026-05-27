package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletHistoryBalanceChangedListener {
    private final SnapshotWalletHistoryService snapshotWalletHistoryService;

    @EventListener
    @Async("walletBalanceProjectionExecutor")
    public void onWalletBalanceChanged(WalletBalanceChangedEvent event) {
        try {
            snapshotWalletHistoryService.recordChangedBalance(event);
        } catch (RuntimeException exception) {
            log.warn("Wallet history balance projection failed. operation=wallet_balance_changed_projection", exception);
        }
    }
}
