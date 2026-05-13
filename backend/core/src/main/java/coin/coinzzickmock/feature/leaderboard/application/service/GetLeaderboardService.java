package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardEntryResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetLeaderboardService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;

    private final LeaderboardProjectionRepository projectionRepository;
    private final List<LeaderboardSnapshotStore> snapshotStores;
    private final PositionPeekTargetTokenCodec targetTokenService;

    @Transactional(readOnly = true)
    public LeaderboardResult get(String modeValue, String limitValue) {
        return get(modeValue, limitValue, null);
    }

    @Transactional(readOnly = true)
    public LeaderboardResult get(String modeValue, String limitValue, Long currentMemberId) {
        LeaderboardMode mode = parseMode(modeValue);
        int limit = parseLimit(limitValue);

        for (LeaderboardSnapshotStore snapshotStore : snapshotStores) {
            LeaderboardResult result = snapshotStore.findSnapshot(mode, limit, currentMemberId)
                    .filter(snapshot -> !snapshot.entries().isEmpty())
                    .map(snapshot -> LeaderboardResult.from(
                            mode,
                            "redis",
                            new LeaderboardSnapshot(snapshot.entries(), snapshot.refreshedAt()),
                            toEntryResults(mode, snapshot.entries(), limit),
                            snapshot.myRank()
                    ))
                    .orElse(null);
            if (result != null) {
                return result;
            }
        }

        List<LeaderboardEntry> entries = projectionRepository.findAll();
        return LeaderboardResult.from(
                mode,
                "database",
                new LeaderboardSnapshot(entries, Instant.now()),
                toEntryResults(mode, entries, limit),
                findDatabaseMyRank(mode, entries, currentMemberId)
        );
    }

    private LeaderboardMode parseMode(String value) {
        return LeaderboardMode.parse(value)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
    }

    private int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LIMIT;
        }

        try {
            int limit = Integer.parseInt(value);
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
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

    private List<LeaderboardEntryResult> toEntryResults(LeaderboardMode mode, List<LeaderboardEntry> entries, int limit) {
        List<LeaderboardEntry> sortedEntries = entries.stream()
                .sorted(comparator(mode))
                .limit(limit)
                .toList();
        return IntStream.range(0, sortedEntries.size())
                .mapToObj(index -> {
                    int rank = index + 1;
                    LeaderboardEntry entry = sortedEntries.get(index);
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
