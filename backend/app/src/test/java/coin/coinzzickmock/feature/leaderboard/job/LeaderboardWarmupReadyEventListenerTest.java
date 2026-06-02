package coin.coinzzickmock.feature.leaderboard.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import coin.coinzzickmock.feature.leaderboard.application.service.RefreshLeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

class LeaderboardWarmupReadyEventListenerTest {
    @Test
    void warmupIsNotGuardedByEnvironmentProperty() {
        ConditionalOnExpression condition = LeaderboardWarmupReadyEventListener.class
                .getAnnotation(ConditionalOnExpression.class);

        assertNull(condition);
    }

    @Test
    void applicationReadyEventDelegatesFullRefreshToApplicationService() {
        RecordingRefreshLeaderboardService refreshLeaderboardService = new RecordingRefreshLeaderboardService();
        LeaderboardWarmupReadyEventListener listener =
                new LeaderboardWarmupReadyEventListener(refreshLeaderboardService);

        listener.warmupAfterApplicationReady();

        assertEquals(1, refreshLeaderboardService.refreshAllCalls);
    }

    private static class RecordingRefreshLeaderboardService extends RefreshLeaderboardService {
        private int refreshAllCalls;

        private RecordingRefreshLeaderboardService() {
            super(null, new coin.coinzzickmock.testsupport.TestLeaderboardSnapshotStore() { });
        }

        @Override
        public void refreshAll() {
            refreshAllCalls++;
        }
    }
}
