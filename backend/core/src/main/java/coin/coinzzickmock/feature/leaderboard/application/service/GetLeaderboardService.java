package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.dto.LeaderboardEntryResult;
import coin.coinzzickmock.feature.leaderboard.application.dto.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.dto.LeaderboardResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import coin.coinzzickmock.feature.positionpeek.application.service.PositionPeekTargetTokenCodec;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetLeaderboardService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final LeaderboardSnapshotStore snapshotStore;
    private final PositionPeekTargetTokenCodec targetTokenService;

    public LeaderboardResult get(LeaderboardMode mode, int limit) {
        return get(mode, limit, null);
    }

    public LeaderboardResult get(LeaderboardMode mode, int limit, Long currentMemberId) {
        try {
            LeaderboardResult snapshotResult = snapshotStore.findSnapshot(mode, limit, currentMemberId)
                    .filter(snapshot -> !snapshot.entries().isEmpty())
                    .map(snapshot -> LeaderboardResult.from(
                            mode,
                            "redis",
                            new LeaderboardSnapshot(snapshot.entries(), snapshot.refreshedAt()),
                            toOrderedEntryResults(mode, snapshot.entries()),
                            snapshot.myRank()
                    ))
                    .orElse(null);
            if (snapshotResult != null) {
                return snapshotResult;
            }
        } catch (RuntimeException exception) {
            log.warn("Leaderboard snapshot read failed. operation=get_leaderboard_snapshot", exception);
        }

        List<LeaderboardEntry> entries = projectionRepository.findAll();
        return LeaderboardResult.from(
                mode,
                "database",
                new LeaderboardSnapshot(entries, Instant.now()),
                toSortedEntryResults(mode, entries, limit),
                findDatabaseMyRank(mode, entries, currentMemberId)
        );
    }

    private Optional<LeaderboardMemberRankResult> findDatabaseMyRank(
            LeaderboardMode mode,
            List<LeaderboardEntry> entries,
            Long currentMemberId
    ) {
        if (currentMemberId == null) {
            return Optional.empty();
        }

        return entries.stream()
                .filter(entry -> entry.memberId().equals(currentMemberId))
                .findFirst()
                .map(currentEntry -> {
                    double currentScore = mode.score(currentEntry);
                    long higherRankedMemberCount = entries.stream()
                            .filter(entry -> mode.score(entry) > currentScore)
                            .count();
                    return LeaderboardMemberRankResult.fromRank(higherRankedMemberCount + 1);
                });
    }

    private List<LeaderboardEntryResult> toSortedEntryResults(
            LeaderboardMode mode,
            List<LeaderboardEntry> entries,
            int limit
    ) {
        List<LeaderboardEntry> sortedEntries = entries.stream()
                .sorted(comparator(mode))
                .limit(limit)
                .toList();
        return toOrderedEntryResults(mode, sortedEntries);
    }

    private List<LeaderboardEntryResult> toOrderedEntryResults(LeaderboardMode mode, List<LeaderboardEntry> entries) {
        return IntStream.range(0, entries.size())
                .mapToObj(index -> {
                    int rank = index + 1;
                    LeaderboardEntry entry = entries.get(index);
                    return LeaderboardEntryResult.from(entry, rank, issueTargetToken(mode, rank, entry));
                })
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

}
