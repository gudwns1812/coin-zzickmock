package coin.coinzzickmock.feature.community.application.implement;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CommunityPostCountDeltaBufferTest {
    private final CommunityPostCountDeltaBuffer buffer = new CommunityPostCountDeltaBuffer();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void drainsNetDeltasByPost() {
        buffer.recordLikeAfterCommit(1L, 1);
        buffer.recordLikeAfterCommit(1L, -1);
        buffer.recordCommentAfterCommit(2L, 1);
        buffer.recordCommentAfterCommit(2L, 1);

        assertThat(buffer.drain())
                .containsExactly(new CommunityPostCountDelta(2L, 0, 2));
        assertThat(buffer.drain()).isEmpty();
    }

    @Test
    void recordsOnlyAfterCommitWhenTransactionSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        buffer.recordLikeAfterCommit(1L, 1);
        assertThat(buffer.drain()).isEmpty();

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        assertThat(buffer.drain())
                .containsExactly(new CommunityPostCountDelta(1L, 1, 0));
    }

    @Test
    void restoresDrainedDeltasWhenFlushFails() {
        List<CommunityPostCountDelta> deltas = List.of(
                new CommunityPostCountDelta(1L, 1, 0),
                new CommunityPostCountDelta(2L, 0, -1)
        );

        buffer.restore(deltas);

        assertThat(buffer.drain()).containsExactlyInAnyOrderElementsOf(deltas);
    }

    @Test
    void recordsConcurrentDeltasByPost() throws InterruptedException {
        int threadCount = 8;
        int iterations = 1_000;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        var executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int thread = 0; thread < threadCount; thread++) {
                executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    for (int index = 0; index < iterations; index++) {
                        buffer.recordLikeAfterCommit(1L, 1);
                        buffer.recordCommentAfterCommit(2L, 1);
                    }
                    done.countDown();
                });
            }
            assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(buffer.drain()).containsExactlyInAnyOrder(
                    new CommunityPostCountDelta(1L, threadCount * iterations, 0),
                    new CommunityPostCountDelta(2L, 0, threadCount * iterations)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
