package coin.coinzzickmock.feature.leaderboard.job;

import coin.coinzzickmock.feature.leaderboard.application.service.RefreshLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaderboardWarmupReadyEventListener {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupAfterApplicationReady() {
        refreshLeaderboardService.refreshAll();
    }
}
