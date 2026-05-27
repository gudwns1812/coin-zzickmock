package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardTradingAccountOpenedListener {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @EventListener
    @Async("walletBalanceProjectionExecutor")
    public void onTradingAccountOpened(TradingAccountOpenedEvent event) {
        try {
            refreshLeaderboardService.recordOpenedAccount(event);
        } catch (RuntimeException exception) {
            log.warn("Leaderboard opened account projection failed. operation=account_opened_projection", exception);
        }
    }
}
