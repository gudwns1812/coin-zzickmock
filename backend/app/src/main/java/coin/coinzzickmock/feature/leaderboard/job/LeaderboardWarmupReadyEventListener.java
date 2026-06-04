package coin.coinzzickmock.feature.leaderboard.job;

import coin.coinzzickmock.feature.leaderboard.application.service.RefreshLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${coin.leaderboard.warmup.enabled:true}' == 'true'")
public class LeaderboardWarmupReadyEventListener {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupAfterApplicationReady() {
        refreshLeaderboardService.refreshAll();
    }
}
