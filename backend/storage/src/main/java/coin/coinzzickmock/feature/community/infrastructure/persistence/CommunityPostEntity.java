package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "community_posts")
public class CommunityPostEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "author_member_id", nullable = false)
    private Long authorMemberId;

    @Column(name = "author_nickname", nullable = false, length = 100)
    private String authorNickname;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CommunityPostEntity() {
    }

    public CommunityPostEntity(
            Long id,
            Long authorMemberId,
            String authorNickname,
            String category,
            String title,
            String contentJson,
            long viewCount,
            long likeCount,
            long commentCount,
            Instant deletedAt
    ) {
        this.id = id;
        this.authorMemberId = authorMemberId;
        this.authorNickname = authorNickname;
        this.category = category;
        this.title = title;
        this.contentJson = contentJson;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.deletedAt = deletedAt;
    }


    public static CommunityPostEntity from(CommunityPost post) {
        return new CommunityPostEntity(null, post.authorMemberId(), post.authorNickname(), post.category().name(),
                post.title(), post.content().value(), post.viewCount(), post.likeCount(), post.commentCount(), post.deletedAt());
    }

    public void apply(CommunityPost post) {
        this.authorMemberId = post.authorMemberId();
        this.authorNickname = post.authorNickname();
        this.category = post.category().name();
        this.title = post.title();
        this.contentJson = post.content().value();
        this.viewCount = post.viewCount();
        this.likeCount = post.likeCount();
        this.commentCount = post.commentCount();
        this.deletedAt = post.deletedAt();
    }

    public CommunityPost toDomain() {
        return CommunityPost.restore(id, authorMemberId, authorNickname, CommunityCategory.valueOf(category), title,
                TiptapJsonDocument.of(contentJson), viewCount, likeCount, commentCount, deletedAt,
                createdAt(), updatedAt(), version);
    }

    public void applyContent(String category, String title, String contentJson) {
        this.category = category;
        this.title = title;
        this.contentJson = contentJson;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (commentCount > 0) {
            this.commentCount--;
        }
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

    public String category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String contentJson() {
        return contentJson;
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

    public long version() {
        return version;
    }
}
