package coin.coinzzickmock.feature.positionpeek.application.service;

import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetResult;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchLeaderboardMembersService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final PositionPeekTargetTokenCodec targetTokenService;

    @Transactional(readOnly = true)
    public List<PositionPeekTargetResult> search(LeaderboardMode mode, String normalizedQuery, int limit) {
        AtomicInteger rank = new AtomicInteger(0);
        return projectionRepository.findAll().stream()
                .sorted(comparator(mode))
                .map(entry -> new RankedEntry(rank.incrementAndGet(), entry))
                .filter(entry -> entry.entry().nickname().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .limit(limit)
                .map(entry -> PositionPeekTargetResult.from(
                        entry.entry(),
                        entry.rank(),
                        issueTargetToken(mode, entry.rank(), entry.entry())
                ))
                .toList();
    }

    private String issueTargetToken(LeaderboardMode mode, int rank, LeaderboardEntry entry) {
        return targetTokenService.issue(new PositionPeekTargetTokenCodec.TargetTokenPayload(
                entry.memberId(),
                rank,
                entry.nickname(),
                entry.walletBalance(),
                entry.profitRate(),
                mode.value()
        ));
    }

    private Comparator<LeaderboardEntry> comparator(LeaderboardMode mode) {
        return Comparator.comparingDouble((LeaderboardEntry entry) -> mode.score(entry))
                .reversed()
                .thenComparing(Comparator.comparingDouble(LeaderboardEntry::walletBalance).reversed())
                .thenComparing(LeaderboardEntry::nickname)
                .thenComparing(LeaderboardEntry::memberId);
    }

    private record RankedEntry(int rank, LeaderboardEntry entry) {
    }
}
