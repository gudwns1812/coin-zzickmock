package coin.coinzzickmock.feature.positionpeek.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekTargetResult;
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
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final LeaderboardProjectionRepository projectionRepository;
    private final PositionPeekTargetTokenCodec targetTokenService;

    @Transactional(readOnly = true)
    public List<PositionPeekTargetResult> search(String modeValue, String query, String limitValue) {
        String normalizedQuery = requireQuery(query);
        LeaderboardMode mode = parseMode(modeValue);
        int limit = parseLimit(limitValue);
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

    private String requireQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return query.trim().toLowerCase(Locale.ROOT);
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
