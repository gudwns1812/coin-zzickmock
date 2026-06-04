package coin.coinzzickmock.feature.community.application.implement;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CommunityPostCountDeltaBuffer {
    private final ConcurrentMap<Long, PendingPostCountDelta> pendingDeltas = new ConcurrentHashMap<>();

    public void recordLikeAfterCommit(Long postId, long delta) {
        recordAfterCommit(postId, delta, 0);
    }

    public void recordCommentAfterCommit(Long postId, long delta) {
        recordAfterCommit(postId, 0, delta);
    }

    public List<CommunityPostCountDelta> drain() {
        if (pendingDeltas.isEmpty()) {
            return List.of();
        }
        List<CommunityPostCountDelta> deltas = new ArrayList<>();
        for (Long postId : pendingDeltas.keySet()) {
            pendingDeltas.computeIfPresent(postId, (id, pending) -> {
                CommunityPostCountDelta delta = pending.toDelta(id);
                if (delta.hasChanges()) {
                    deltas.add(delta);
                }
                return null;
            });
        }
        return deltas;
    }

    public void restore(Collection<CommunityPostCountDelta> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        deltas.stream()
                .filter(CommunityPostCountDelta::hasChanges)
                .forEach(delta -> addDelta(delta.postId(), delta.likeDelta(), delta.commentDelta()));
    }

    private void recordAfterCommit(Long postId, long likeDelta, long commentDelta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            addDelta(postId, likeDelta, commentDelta);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                addDelta(postId, likeDelta, commentDelta);
            }
        });
    }

    private void addDelta(Long postId, long likeDelta, long commentDelta) {
        if (postId == null || (likeDelta == 0 && commentDelta == 0)) {
            return;
        }
        pendingDeltas.compute(postId, (id, pending) -> {
            PendingPostCountDelta next = pending == null ? new PendingPostCountDelta() : pending;
            next.add(likeDelta, commentDelta);
            return next.hasChanges() ? next : null;
        });
    }

    private static final class PendingPostCountDelta {
        private long likeDelta;
        private long commentDelta;

        private void add(long likeDelta, long commentDelta) {
            this.likeDelta += likeDelta;
            this.commentDelta += commentDelta;
        }

        private boolean hasChanges() {
            return likeDelta != 0 || commentDelta != 0;
        }

        private CommunityPostCountDelta toDelta(Long postId) {
            return new CommunityPostCountDelta(postId, likeDelta, commentDelta);
        }
    }
}
