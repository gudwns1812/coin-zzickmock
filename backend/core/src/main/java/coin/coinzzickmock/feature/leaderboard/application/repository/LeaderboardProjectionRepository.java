package coin.coinzzickmock.feature.leaderboard.application.repository;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

public interface LeaderboardProjectionRepository {
    List<LeaderboardEntry> findAll();

    Optional<LeaderboardEntry> findByMemberId(Long memberId);

    List<LeaderboardEntry> findTop(int limit);

    Optional<Long> findRankByMemberId(Long memberId);
}


