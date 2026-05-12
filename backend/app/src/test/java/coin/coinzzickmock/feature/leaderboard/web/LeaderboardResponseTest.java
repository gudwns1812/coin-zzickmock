package coin.coinzzickmock.feature.leaderboard.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardEntryResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LeaderboardResponseTest {
    @Test
    void doesNotExposeInternalMemberId() {
        LeaderboardResponse response = LeaderboardResponse.from(new LeaderboardResult(
                "profitRate",
                "database",
                Instant.parse("2026-04-26T00:00:00Z"),
                List.of(new LeaderboardEntryResult(1, "Demo", 124_580, 0.2458)),
                Optional.empty()
        ));

        assertTrue(response.toString().contains("Demo"));
        assertFalse(response.toString().contains("member-"));
    }
}
