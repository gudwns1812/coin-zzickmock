package coin.coinzzickmock.feature.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import coin.coinzzickmock.feature.community.application.implement.CommunityPostCountDeltaBuffer;
import coin.coinzzickmock.feature.community.application.query.ListCommunityPostsQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlushCommunityPostCountDeltasServiceTest {
    @Test
    void restoresDrainedDeltasWhenRepositoryFlushFails() {
        CommunityPostCountDeltaBuffer buffer = new CommunityPostCountDeltaBuffer();
        buffer.recordLikeAfterCommit(1L, 1);
        FlushCommunityPostCountDeltasService service = new FlushCommunityPostCountDeltasService(
                buffer,
                new FailingPostRepository()
        );

        assertThatThrownBy(service::flush).isInstanceOf(IllegalStateException.class);

        assertThat(buffer.drain()).containsExactly(new CommunityPostCountDelta(1L, 1, 0));
    }

    private static final class FailingPostRepository implements CommunityPostRepository {
        @Override
        public List<CommunityPost> findLatestNotices(int limit) {
            return List.of();
        }

        @Override
        public CommunityPostPage findPosts(ListCommunityPostsQuery query) {
            return new CommunityPostPage(List.of(), 0, 20, 0, 0, false);
        }

        @Override
        public Optional<CommunityPost> findActiveById(Long postId) {
            return Optional.empty();
        }

        @Override
        public CommunityPost save(CommunityPost post) {
            return post;
        }

        @Override
        public CommunityPost update(CommunityPost post) {
            return post;
        }

        @Override
        public void softDelete(Long postId, Instant deletedAt) {
        }

        @Override
        public void incrementViewCount(Long postId) {
        }

        @Override
        public void applyCountDeltas(Collection<CommunityPostCountDelta> deltas) {
            throw new IllegalStateException("flush failed");
        }
    }
}
