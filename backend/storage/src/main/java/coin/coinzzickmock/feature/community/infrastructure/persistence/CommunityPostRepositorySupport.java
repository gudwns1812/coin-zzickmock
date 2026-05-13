package coin.coinzzickmock.feature.community.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommunityPostRepositorySupport {
    private final CommunityPostEntityRepository postEntityRepository;

    void decrementCommentCount(Long postId) {
        postEntityRepository.findWithLockingById(postId).ifPresent(CommunityPostEntity::decrementCommentCount);
    }
}
