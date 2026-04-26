package coin.coinzzickmock.feature.leaderboard.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardWalletBalanceChangedListenerTest {
    @Test
    void walletBalanceChangedEventRefreshesSingleLeaderboardMember() {
        RecordingRefreshLeaderboardService refreshLeaderboardService = new RecordingRefreshLeaderboardService();
        LeaderboardWalletBalanceChangedListener listener = new LeaderboardWalletBalanceChangedListener(refreshLeaderboardService);

        listener.onWalletBalanceChanged(new WalletBalanceChangedEvent("demo-member"));

        assertEquals(List.of("demo-member"), refreshLeaderboardService.memberIds);
    }

    private static class RecordingRefreshLeaderboardService extends RefreshLeaderboardService {
        private final List<String> memberIds = new ArrayList<>();

        private RecordingRefreshLeaderboardService() {
            super(null, List.of());
        }

        @Override
        public void refreshMember(String memberId) {
            memberIds.add(memberId);
        }
    }
}
