package coin.coinzzickmock.feature.leaderboard.infrastructure.config;

import coin.coinzzickmock.feature.leaderboard.application.service.RefreshLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${coin.leaderboard.redis.enabled:false}' == 'true' && '${coin.leaderboard.refresh.enabled:false}' == 'true'")
public class LeaderboardRefreshScheduler {
    private final RefreshLeaderboardService refreshLeaderboardService;

    @Scheduled(
            fixedDelayString = "${coin.leaderboard.refresh.delay-ms:3600000}",
            initialDelayString = "${coin.leaderboard.refresh.initial-delay-ms:0}"
    )
    public void refresh() {
        refreshLeaderboardService.refreshAll();
    }
}
