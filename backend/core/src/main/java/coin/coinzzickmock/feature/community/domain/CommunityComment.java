package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;
import java.util.Objects;

public final class CommunityComment {
    private static final int MAX_CONTENT_LENGTH = 1_000;

    private final Long id;
    private final Long postId;
    private final Long authorMemberId;
    private final String authorNickname;
    private final String content;
    private final Instant deletedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CommunityComment(
            Long id,
            Long postId,
            Long authorMemberId,
            String authorNickname,
            String content,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.postId = postId;
        this.authorMemberId = authorMemberId;
        this.authorNickname = authorNickname;
        this.content = content;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CommunityComment create(
            Long postId,
            Long authorMemberId,
            String authorNickname,
            String content,
            Instant now
    ) {
        if (postId == null || postId <= 0 || authorMemberId == null || authorMemberId <= 0 || authorNickname == null || authorNickname.isBlank()) {
            throw invalid();
        }
        return new CommunityComment(
                null,
                postId,
                authorMemberId,
                authorNickname.trim(),
                normalizeContent(content),
                null,
                Objects.requireNonNull(now, "now"),
                now
        );
    }

    public CommunityComment withId(Long id) {
        return new CommunityComment(id, postId, authorMemberId, authorNickname, content, deletedAt, createdAt, updatedAt);
    }

    public CommunityComment rewrite(String nextContent, Instant now) {
        return new CommunityComment(
                id,
                postId,
                authorMemberId,
                authorNickname,
                normalizeContent(nextContent),
                deletedAt,
                createdAt,
                Objects.requireNonNull(now, "now")
        );
    }

    public CommunityComment softDelete(Instant now) {
        return new CommunityComment(
                id,
                postId,
                authorMemberId,
                authorNickname,
                content,
                Objects.requireNonNull(now, "now"),
                createdAt,
                now
        );
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            throw invalid();
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw invalid();
        }
        return trimmed;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    public Long id() {
        return id;
    }

    public Long postId() {
        return postId;
    }

    public Long authorMemberId() {
        return authorMemberId;
    }

    public String authorNickname() {
        return authorNickname;
    }

    public String content() {
        return content;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
