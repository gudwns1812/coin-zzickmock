package coin.coinzzickmock.feature.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeEntityRepository
        extends JpaRepository<CommunityPostLikeEntity, CommunityPostLikeEntity.Key> {
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    long deleteByPostIdAndMemberId(Long postId, Long memberId);
}
