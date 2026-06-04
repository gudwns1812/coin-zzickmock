package coin.coinzzickmock.feature.community.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostEntityRepository extends JpaRepository<CommunityPostEntity, Long> {
    Page<CommunityPostEntity> findByCategoryAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String category, Pageable pageable);

    Page<CommunityPostEntity> findByCategoryNotAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            String category,
            Pageable pageable
    );

    Optional<CommunityPostEntity> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Locks by physical row id including soft-deleted posts. Prefer
     * {@link #findWithLockingByIdAndDeletedAtIsNull(Long)} for community mutations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommunityPostEntity> findWithLockingById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommunityPostEntity> findWithLockingByIdAndDeletedAtIsNull(Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
            update CommunityPostEntity post
               set post.viewCount = post.viewCount + 1
             where post.id = :postId
               and post.deletedAt is null
            """)
    int incrementViewCountIfActive(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update CommunityPostEntity post
               set post.likeCount = case
                       when post.likeCount + :likeDelta > 0 then post.likeCount + :likeDelta
                       else 0
                   end,
                   post.commentCount = case
                       when post.commentCount + :commentDelta > 0 then post.commentCount + :commentDelta
                       else 0
                   end
             where post.id = :postId
               and post.deletedAt is null
            """)
    int applyCountDeltaIfActive(
            @Param("postId") Long postId,
            @Param("likeDelta") long likeDelta,
            @Param("commentDelta") long commentDelta
    );
}
