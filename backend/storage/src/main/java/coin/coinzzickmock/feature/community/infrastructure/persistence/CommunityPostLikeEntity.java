package coin.coinzzickmock.feature.community.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@IdClass(CommunityPostLikeEntity.Key.class)
@Table(name = "community_post_likes")
public class CommunityPostLikeEntity {
    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommunityPostLikeEntity() {
    }

    public CommunityPostLikeEntity(Long postId, Long memberId, Instant createdAt) {
        this.postId = postId;
        this.memberId = memberId;
        this.createdAt = createdAt;
    }

    public Long postId() {
        return postId;
    }

    public Long memberId() {
        return memberId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public static class Key implements Serializable {
        private Long postId;
        private Long memberId;

        protected Key() {
        }

        public Key(Long postId, Long memberId) {
            this.postId = postId;
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(postId, key.postId) && Objects.equals(memberId, key.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, memberId);
        }
    }
}
