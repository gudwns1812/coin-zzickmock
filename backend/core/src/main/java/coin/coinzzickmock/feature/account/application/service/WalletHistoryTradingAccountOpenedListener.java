package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletHistoryTradingAccountOpenedListener {
    private final SnapshotWalletHistoryService snapshotWalletHistoryService;

    @EventListener
    @Async("walletBalanceProjectionExecutor")
    public void onTradingAccountOpened(TradingAccountOpenedEvent event) {
        try {
            snapshotWalletHistoryService.recordOpenedAccount(event);
        } catch (RuntimeException exception) {
            log.warn("Wallet history baseline creation failed. operation=account_opened_projection", exception);
        }
    }
}
