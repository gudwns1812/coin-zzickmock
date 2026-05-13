package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "community_comments")
public class CommunityCommentEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_member_id", nullable = false)
    private Long authorMemberId;

    @Column(name = "author_nickname", nullable = false, length = 100)
    private String authorNickname;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CommunityCommentEntity() {
    }

    public CommunityCommentEntity(
            Long id,
            Long postId,
            Long authorMemberId,
            String authorNickname,
            String content,
            Instant deletedAt
    ) {
        this.id = id;
        this.postId = postId;
        this.authorMemberId = authorMemberId;
        this.authorNickname = authorNickname;
        this.content = content;
        this.deletedAt = deletedAt;
    }


    public static CommunityCommentEntity from(CommunityComment comment) {
        return new CommunityCommentEntity(null, comment.postId(), comment.authorMemberId(), comment.authorNickname(),
                comment.content(), comment.deletedAt());
    }

    public void apply(CommunityComment comment) {
        this.postId = comment.postId();
        this.authorMemberId = comment.authorMemberId();
        this.authorNickname = comment.authorNickname();
        this.content = comment.content();
        this.deletedAt = comment.deletedAt();
    }

    public CommunityComment toDomain() {
        return CommunityComment.restore(id, postId, authorMemberId, authorNickname, content, deletedAt, createdAt(), updatedAt());
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
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
}
