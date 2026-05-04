package coin.coinzzickmock.feature.leaderboard.infrastructure.store;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LeaderboardSnapshotStoreAdapterTest {
    @Test
    void readsRedisZsetInDescendingScoreOrder() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/coin/coinzzickmock/feature/leaderboard/infrastructure/store/LeaderboardSnapshotStoreAdapter.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("reverseRangeWithScores"));
    }

    @Test
    void findsMyRankWithRedisReverseRank() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/coin/coinzzickmock/feature/leaderboard/infrastructure/store/LeaderboardSnapshotStoreAdapter.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("reverseRank"));
        assertTrue(source.contains("zeroBasedRank + 1"));
    }
}
