package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record CommunityComment(
        Long id,
        Long postId,
        CommunityAuthor author,
        CommunityCommentContent content,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public CommunityComment {
        if (postId == null || postId <= 0 || author == null || content == null || createdAt == null) {
            throw invalid();
        }
    }

    public static CommunityComment create(Long postId, CommunityAuthor author, CommunityCommentContent content, Instant createdAt) {
        return new CommunityComment(null, postId, author, content, createdAt, createdAt, null);
    }

    public static CommunityComment restore(
            Long id,
            Long postId,
            CommunityAuthor author,
            CommunityCommentContent content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (id == null || id <= 0) {
            throw invalid();
        }
        return new CommunityComment(id, postId, author, content, createdAt, updatedAt, deletedAt);
    }

    public CommunityComment softDelete(Instant deletedAt) {
        if (deleted()) {
            throw invalid();
        }
        Instant occurredAt = requireTime(deletedAt);
        return new CommunityComment(id, postId, author, content, createdAt, occurredAt, occurredAt);
    }

    public boolean authoredBy(Long memberId) {
        return author.memberId().equals(memberId);
    }

    public boolean deleted() {
        return deletedAt != null;
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
