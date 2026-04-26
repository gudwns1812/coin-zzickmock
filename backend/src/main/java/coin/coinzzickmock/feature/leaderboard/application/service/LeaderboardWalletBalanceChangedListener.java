package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaderboardWalletBalanceChangedListener {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @EventListener
    public void onWalletBalanceChanged(WalletBalanceChangedEvent event) {
        refreshLeaderboardService.refreshMember(event.memberId());
    }
}
