package coin.coinzzickmock.feature.account.job;

import coin.coinzzickmock.feature.account.application.service.SnapshotWalletHistoryService;
import coin.coinzzickmock.feature.account.application.service.WalletHistoryRolloverLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "coin.account.wallet-history.rollover",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WalletHistoryRolloverScheduler {
    private final SnapshotWalletHistoryService snapshotWalletHistoryService;
    private final WalletHistoryRolloverLock walletHistoryRolloverLock;

    @Scheduled(cron = "${coin.account.wallet-history.rollover.cron:0 0 0 * * *}", zone = "Asia/Seoul")
    public void rolloverKstDay() {
        boolean executed = walletHistoryRolloverLock.runIfAcquired(
                snapshotWalletHistoryService::createTodayBaselines
        );
        if (!executed) {
            log.info("Wallet history rollover skipped because another instance owns the lock.");
        }
    }
}
