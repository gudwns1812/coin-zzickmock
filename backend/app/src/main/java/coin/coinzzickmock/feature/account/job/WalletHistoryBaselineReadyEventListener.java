package coin.coinzzickmock.feature.account.job;

import coin.coinzzickmock.feature.account.application.service.SnapshotWalletHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "coin.account.wallet-history.startup-baseline",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WalletHistoryBaselineReadyEventListener {
    private final SnapshotWalletHistoryService snapshotWalletHistoryService;

    @EventListener(ApplicationReadyEvent.class)
    public void createTodayBaselinesAfterApplicationReady() {
        snapshotWalletHistoryService.createTodayBaselines();
    }
}
