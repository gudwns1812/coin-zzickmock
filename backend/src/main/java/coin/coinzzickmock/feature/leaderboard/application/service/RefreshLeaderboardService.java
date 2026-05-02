package coin.coinzzickmock.feature.leaderboard.application.service;

import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshLeaderboardService {
    private final LeaderboardProjectionRepository projectionRepository;
    private final List<LeaderboardSnapshotStore> snapshotStores;

    @Transactional(readOnly = true)
    public void refreshAll() {
        if (snapshotStores.isEmpty()) {
            return;
        }

        LeaderboardSnapshot snapshot = new LeaderboardSnapshot(projectionRepository.findAll(), Instant.now());
        for (LeaderboardSnapshotStore snapshotStore : snapshotStores) {
            try {
                snapshotStore.replace(snapshot);
            } catch (RuntimeException exception) {
                log.warn("Leaderboard full refresh failed.", exception);
            }
        }
    }

    @Transactional(readOnly = true)
    public void refreshMember(Long memberId) {
        if (snapshotStores.isEmpty()) {
            return;
        }

        LeaderboardEntry entry = projectionRepository.findByMemberId(memberId).orElse(null);
        if (entry == null) {
            removeMember(memberId);
            return;
        }

        for (LeaderboardSnapshotStore snapshotStore : snapshotStores) {
            try {
                snapshotStore.update(entry);
            } catch (RuntimeException exception) {
                log.warn("Leaderboard member refresh failed. memberId={}", memberId, exception);
            }
        }
    }

    private void removeMember(Long memberId) {
        for (LeaderboardSnapshotStore snapshotStore : snapshotStores) {
            try {
                snapshotStore.remove(memberId);
            } catch (RuntimeException exception) {
                log.warn("Leaderboard member removal failed. memberId={}", memberId, exception);
            }
        }
    }
}
