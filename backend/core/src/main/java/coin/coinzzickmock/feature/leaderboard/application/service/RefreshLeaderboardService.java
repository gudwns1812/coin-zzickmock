package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshLeaderboardService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final LeaderboardSnapshotStore snapshotStore;

    @Transactional(readOnly = true)
    public void refreshAll() {
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot(projectionRepository.findAll(), Instant.now());
        try {
            snapshotStore.replace(snapshot);
        } catch (RuntimeException exception) {
            log.warn("Leaderboard full refresh failed.", exception);
        }
    }

    public void recordOpenedAccount(TradingAccountOpenedEvent event) {
        LeaderboardEntry entry = LeaderboardEntry.fromWalletBalance(
                event.memberId(),
                event.nickname(),
                event.walletBalance(),
                event.openedAt()
        );
        try {
            snapshotStore.update(entry);
        } catch (RuntimeException exception) {
            log.warn("Leaderboard opened account projection failed. operation=account_opened_projection",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public void refreshMember(Long memberId) {
        LeaderboardEntry entry = projectionRepository.findByMemberId(memberId).orElse(null);
        if (entry == null) {
            removeMember(memberId);
            return;
        }

        try {
            snapshotStore.update(entry);
        } catch (RuntimeException exception) {
            log.warn("Leaderboard member refresh failed. operation=refresh_member", exception);
        }
    }

    private void removeMember(Long memberId) {
        try {
            snapshotStore.remove(memberId);
        } catch (RuntimeException exception) {
            log.warn("Leaderboard member removal failed. operation=remove_member", exception);
        }
    }
}
