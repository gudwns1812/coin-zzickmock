package coin.coinzzickmock.feature.leaderboard.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.leaderboard.application.service.RefreshLeaderboardService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class LeaderboardRefreshSchedulerTest {
    @Test
    void fullRefreshRunsHourlyByDefault() throws NoSuchMethodException {
        Method method = LeaderboardRefreshScheduler.class.getDeclaredMethod("refresh");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${coin.leaderboard.refresh.delay-ms:3600000}", scheduled.fixedDelayString());
    }

    @Test
    void delegatesFullRefreshToApplicationService() {
        RecordingRefreshLeaderboardService refreshLeaderboardService = new RecordingRefreshLeaderboardService();
        LeaderboardRefreshScheduler scheduler = new LeaderboardRefreshScheduler(refreshLeaderboardService);

        scheduler.refresh();

        assertEquals(1, refreshLeaderboardService.refreshAllCalls);
    }

    private static class RecordingRefreshLeaderboardService extends RefreshLeaderboardService {
        private int refreshAllCalls;

        private RecordingRefreshLeaderboardService() {
            super(null, List.of());
        }

        @Override
        public void refreshAll() {
            refreshAllCalls++;
        }
    }
}
