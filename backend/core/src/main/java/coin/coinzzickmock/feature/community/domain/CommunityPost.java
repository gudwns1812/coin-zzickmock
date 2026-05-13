package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.domain.content.TiptapContentJson;
import java.time.Instant;

public record CommunityPost(
        Long id,
        CommunityAuthor author,
        CommunityPostCategory category,
        CommunityPostTitle title,
        TiptapContentJson content,
        CommunityPostCounts counts,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public CommunityPost {
        if (author == null || category == null || title == null || content == null || counts == null || createdAt == null) {
            throw invalid();
        }
    }

    public static CommunityPost create(
            CommunityAuthor author,
            CommunityPostCategory category,
            CommunityPostTitle title,
            TiptapContentJson content,
            Instant createdAt
    ) {
        return new CommunityPost(null, author, category, title, content, CommunityPostCounts.zero(), createdAt, createdAt, null);
    }

    public static CommunityPost restore(
            Long id,
            CommunityAuthor author,
            CommunityPostCategory category,
            CommunityPostTitle title,
            TiptapContentJson content,
            CommunityPostCounts counts,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (id == null || id <= 0) {
            throw invalid();
        }
        return new CommunityPost(id, author, category, title, content, counts, createdAt, updatedAt, deletedAt);
    }

    public CommunityPost revise(CommunityPostCategory category, CommunityPostTitle title, TiptapContentJson content, Instant updatedAt) {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts, createdAt, requireTime(updatedAt), deletedAt);
    }

    public CommunityPost softDelete(Instant deletedAt) {
        requireActive();
        Instant occurredAt = requireTime(deletedAt);
        return new CommunityPost(id, author, category, title, content, counts, createdAt, occurredAt, occurredAt);
    }

    public CommunityPost increaseViewCount() {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts.viewed(), createdAt, updatedAt, deletedAt);
    }

    public CommunityPost increaseLikeCount() {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts.liked(), createdAt, updatedAt, deletedAt);
    }

    public CommunityPost decreaseLikeCount() {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts.unliked(), createdAt, updatedAt, deletedAt);
    }

    public CommunityPost increaseCommentCount() {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts.commented(), createdAt, updatedAt, deletedAt);
    }

    public CommunityPost decreaseCommentCount() {
        requireActive();
        return new CommunityPost(id, author, category, title, content, counts.uncommented(), createdAt, updatedAt, deletedAt);
    }

    public boolean authoredBy(Long memberId) {
        return author.memberId().equals(memberId);
    }

    public boolean deleted() {
        return deletedAt != null;
    }

    private void requireActive() {
        if (deleted()) {
            throw invalid();
        }
    }

    private static Instant requireTime(Instant value) {
        if (value == null) {
            throw invalid();
        }
        return value;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
