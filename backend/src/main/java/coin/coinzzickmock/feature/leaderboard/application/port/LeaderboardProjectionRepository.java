package coin.coinzzickmock.feature.leaderboard.application.port;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import java.util.List;
import java.util.Optional;

public interface LeaderboardProjectionRepository {
    List<LeaderboardEntry> findAll();

    Optional<LeaderboardEntry> findByMemberId(Long memberId);
}
