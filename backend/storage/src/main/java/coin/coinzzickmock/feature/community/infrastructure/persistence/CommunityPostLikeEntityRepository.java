package coin.coinzzickmock.feature.community.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostLikeEntityRepository
        extends JpaRepository<CommunityPostLikeEntity, CommunityPostLikeEntity.Key> {
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert ignore into community_post_likes (post_id, member_id, created_at)
            values (:postId, :memberId, :createdAt)
            """, nativeQuery = true)
    int insertIgnore(
            @Param("postId") Long postId,
            @Param("memberId") Long memberId,
            @Param("createdAt") Instant createdAt
    );

    long deleteByPostIdAndMemberId(Long postId, Long memberId);
}
