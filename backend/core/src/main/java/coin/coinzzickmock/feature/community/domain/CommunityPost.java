package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;
import java.util.Objects;

public final class CommunityPost {
    private static final int MAX_TITLE_LENGTH = 200;

    private final Long id;
    private final Long authorMemberId;
    private final String authorNickname;
    private final CommunityCategory category;
    private final String title;
    private final TiptapJsonDocument content;
    private final long viewCount;
    private final long likeCount;
    private final long commentCount;
    private final Instant deletedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private CommunityPost(
            Long id,
            Long authorMemberId,
            String authorNickname,
            CommunityCategory category,
            String title,
            TiptapJsonDocument content,
            long viewCount,
            long likeCount,
            long commentCount,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.authorMemberId = authorMemberId;
        this.authorNickname = authorNickname;
        this.category = category;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static CommunityPost create(
            Long authorMemberId,
            String authorNickname,
            CommunityCategory category,
            String title,
            TiptapJsonDocument content,
            Instant now
    ) {
        requireAuthor(authorMemberId, authorNickname);
        requireCategory(category);
        return new CommunityPost(
                null,
                authorMemberId,
                authorNickname.trim(),
                category,
                requireTitle(title),
                Objects.requireNonNull(content, "content"),
                0,
                0,
                0,
                null,
                Objects.requireNonNull(now, "now"),
                now,
                0
        );
    }

    public static CommunityPost restore(
            Long id,
            Long authorMemberId,
            String authorNickname,
            CommunityCategory category,
            String title,
            TiptapJsonDocument content,
            long viewCount,
            long likeCount,
            long commentCount,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        requireAuthor(authorMemberId, authorNickname);
        requireCategory(category);
        if (id == null || id <= 0 || viewCount < 0 || likeCount < 0 || commentCount < 0 || createdAt == null) {
            throw invalid();
        }
        return new CommunityPost(id, authorMemberId, authorNickname.trim(), category, requireTitle(title),
                Objects.requireNonNull(content, "content"), viewCount, likeCount, commentCount, deletedAt,
                createdAt, updatedAt == null ? createdAt : updatedAt, version);
    }

    public CommunityPost withId(Long id) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                title,
                content,
                viewCount,
                likeCount,
                commentCount,
                deletedAt,
                createdAt,
                updatedAt,
                version
        );
    }

    public CommunityPost rename(String nextTitle, Instant now) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                requireTitle(nextTitle),
                content,
                viewCount,
                likeCount,
                commentCount,
                deletedAt,
                createdAt,
                Objects.requireNonNull(now, "now"),
                version
        );
    }

    public CommunityPost recategorize(CommunityCategory nextCategory, Instant now) {
        requireCategory(nextCategory);
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                nextCategory,
                title,
                content,
                viewCount,
                likeCount,
                commentCount,
                deletedAt,
                createdAt,
                Objects.requireNonNull(now, "now"),
                version
        );
    }

    public CommunityPost rewriteContent(TiptapJsonDocument nextContent, Instant now) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                title,
                Objects.requireNonNull(nextContent, "nextContent"),
                viewCount,
                likeCount,
                commentCount,
                deletedAt,
                createdAt,
                Objects.requireNonNull(now, "now"),
                version
        );
    }

    public CommunityPost incrementViewCount(Instant now) {
        return counted(viewCount + 1, likeCount, commentCount, now);
    }

    public CommunityPost incrementLikeCount(Instant now) {
        return counted(viewCount, likeCount + 1, commentCount, now);
    }

    public CommunityPost decrementLikeCount(Instant now) {
        if (likeCount == 0) {
            throw invalid();
        }
        return counted(viewCount, likeCount - 1, commentCount, now);
    }

    public CommunityPost incrementCommentCount(Instant now) {
        return counted(viewCount, likeCount, commentCount + 1, now);
    }

    public CommunityPost decrementCommentCount(Instant now) {
        if (commentCount == 0) {
            throw invalid();
        }
        return counted(viewCount, likeCount, commentCount - 1, now);
    }

    public CommunityPost softDelete(Instant now) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                title,
                content,
                viewCount,
                likeCount,
                commentCount,
                Objects.requireNonNull(now, "now"),
                createdAt,
                now,
                version
        );
    }

    public CommunityPost withVersion(long nextVersion) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                title,
                content,
                viewCount,
                likeCount,
                commentCount,
                deletedAt,
                createdAt,
                updatedAt,
                nextVersion
        );
    }

    private CommunityPost counted(long nextViewCount, long nextLikeCount, long nextCommentCount, Instant now) {
        return new CommunityPost(
                id,
                authorMemberId,
                authorNickname,
                category,
                title,
                content,
                nextViewCount,
                nextLikeCount,
                nextCommentCount,
                deletedAt,
                createdAt,
                Objects.requireNonNull(now, "now"),
                version
        );
    }

    private static String requireTitle(String title) {
        if (title == null) {
            throw invalid();
        }
        String trimmed = title.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TITLE_LENGTH) {
            throw invalid();
        }
        return trimmed;
    }

    private static void requireAuthor(Long authorMemberId, String authorNickname) {
        if (authorMemberId == null || authorMemberId <= 0 || authorNickname == null || authorNickname.isBlank()) {
            throw invalid();
        }
    }

    private static void requireCategory(CommunityCategory category) {
        if (category == null) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    public Long id() {
        return id;
    }

    public Long authorMemberId() {
        return authorMemberId;
    }

    public String authorNickname() {
        return authorNickname;
    }

    public CommunityCategory category() {
        return category;
    }

    public String title() {
        return title;
    }

    public TiptapJsonDocument content() {
        return content;
    }

    public long viewCount() {
        return viewCount;
    }

    public long likeCount() {
        return likeCount;
    }

    public long commentCount() {
        return commentCount;
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

    public long version() {
        return version;
    }
}
