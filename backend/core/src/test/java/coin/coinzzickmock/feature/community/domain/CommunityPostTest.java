package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommunityPostTest {
    @Test
    void createsAndUpdatesStateWithoutBreakingCountInvariants() {
        TiptapJsonDocument content = TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"hello"}]}]}
                """);

        CommunityPost post = CommunityPost.create(1L, "  writer  ", CommunityCategory.CHAT, "  title  ", content, Instant.parse("2026-05-13T00:00:00Z"));

        assertThat(post.authorNickname()).isEqualTo("writer");
        assertThat(post.title()).isEqualTo("title");
        assertThat(post.viewCount()).isZero();
        assertThat(post.incrementViewCount(Instant.parse("2026-05-13T00:01:00Z")).viewCount()).isEqualTo(1);
        assertThat(post.incrementLikeCount(Instant.parse("2026-05-13T00:01:00Z")).likeCount()).isEqualTo(1);
        assertThat(post.incrementCommentCount(Instant.parse("2026-05-13T00:01:00Z")).commentCount()).isEqualTo(1);
        assertThat(post.softDelete(Instant.parse("2026-05-13T00:02:00Z")).deletedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidCountUnderflowAndBlankContent() {
        TiptapJsonDocument content = TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"hello"}]}]}
                """);
        CommunityPost post = CommunityPost.create(1L, "writer", CommunityCategory.CHAT, "title", content, Instant.parse("2026-05-13T00:00:00Z"));

        assertThrows(CoreException.class, () -> post.decrementLikeCount(Instant.now()));
        assertThrows(CoreException.class, () -> CommunityComment.create(1L, 1L, "writer", "   ", Instant.now()));
    }
}
