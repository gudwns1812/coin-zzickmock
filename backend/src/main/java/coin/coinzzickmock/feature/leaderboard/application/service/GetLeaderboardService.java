package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardEntryResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
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

    @Transactional(readOnly = true)
    public LeaderboardResult get(String modeValue, String limitValue) {
        return get(modeValue, limitValue, Optional.empty());
    }

    @Transactional(readOnly = true)
    public LeaderboardResult get(String modeValue, String limitValue, Optional<Long> currentMemberId) {
        LeaderboardMode mode = parseMode(modeValue);
        int limit = parseLimit(limitValue);

        for (LeaderboardSnapshotStore snapshotStore : snapshotStores) {
            LeaderboardResult result = snapshotStore.findSnapshot(mode, limit, currentMemberId)
                    .filter(snapshot -> !snapshot.entries().isEmpty())
                    .map(snapshot -> toResult(mode, "redis", snapshot, limit))
                    .orElse(null);
            if (result != null) {
                return result;
            }
        }

        List<LeaderboardEntry> entries = projectionRepository.findAll();
        return toResult(
                mode,
                "database",
                new LeaderboardSnapshot(entries, Instant.now()),
                limit,
                findDatabaseMyRank(mode, entries, currentMemberId)
        );
    }

    private LeaderboardMode parseMode(String value) {
        return LeaderboardMode.parse(value)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST, "지원하지 않는 랭킹 모드입니다."));
    }

    private int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LIMIT;
        }

        try {
            int limit = Integer.parseInt(value);
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new CoreException(ErrorCode.INVALID_REQUEST, "랭킹 조회 개수는 1~50 사이여야 합니다.");
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "랭킹 조회 개수는 숫자여야 합니다.");
        }
    }

    private LeaderboardResult toResult(
            LeaderboardMode mode,
            String source,
            LeaderboardSnapshotResult snapshot,
            int limit
    ) {
        return toResult(
                mode,
                source,
                new LeaderboardSnapshot(snapshot.entries(), snapshot.refreshedAt()),
                limit,
                snapshot.myRank()
        );
    }

    private LeaderboardResult toResult(
            LeaderboardMode mode,
            String source,
            LeaderboardSnapshot snapshot,
            int limit,
            Optional<LeaderboardMemberRankResult> myRank
    ) {
        List<LeaderboardEntryResult> entries = snapshot.entries().stream()
                .sorted(comparator(mode))
                .limit(limit)
                .map(entry -> new LeaderboardEntryResult(
                        0,
                        entry.nickname(),
                        entry.walletBalance(),
                        entry.profitRate()
                ))
                .toList();

        return new LeaderboardResult(
                mode.value(),
                source,
                snapshot.refreshedAt(),
                IntStream.range(0, entries.size())
                        .mapToObj(index -> {
                            LeaderboardEntryResult entry = entries.get(index);
                            return new LeaderboardEntryResult(
                                    index + 1,
                                    entry.nickname(),
                                    entry.walletBalance(),
                                    entry.profitRate()
                            );
                        })
                        .toList(),
                myRank
        );
    }

    private Optional<LeaderboardMemberRankResult> findDatabaseMyRank(
            LeaderboardMode mode,
            List<LeaderboardEntry> entries,
            Optional<Long> currentMemberId
    ) {
        if (currentMemberId.isEmpty()) {
            return Optional.empty();
        }

        Long memberId = currentMemberId.get();
        List<LeaderboardEntry> rankedEntries = entries.stream()
                .sorted(databaseMyRankComparator(mode))
                .toList();

        return IntStream.range(0, rankedEntries.size())
                .filter(index -> rankedEntries.get(index).memberId().equals(memberId))
                .findFirst()
                .stream()
                .mapToObj(index -> new LeaderboardMemberRankResult(index + 1))
                .findFirst();
    }

    private Comparator<LeaderboardEntry> comparator(LeaderboardMode mode) {
        return Comparator.comparingDouble((LeaderboardEntry entry) -> mode.score(entry))
                .reversed()
                .thenComparing(Comparator.comparingDouble(LeaderboardEntry::walletBalance).reversed())
                .thenComparing(LeaderboardEntry::nickname)
                .thenComparing(LeaderboardEntry::memberId);
    }

    private Comparator<LeaderboardEntry> databaseMyRankComparator(LeaderboardMode mode) {
        return Comparator.comparingDouble((LeaderboardEntry entry) -> mode.score(entry))
                .reversed()
                .thenComparing(LeaderboardEntry::memberId);
    }
}
