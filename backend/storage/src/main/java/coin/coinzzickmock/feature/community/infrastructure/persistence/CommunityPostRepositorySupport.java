package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CommunityPostRepositorySupport {
    private final CommunityPostEntityRepository postEntityRepository;

    @Transactional
    void decrementCommentCount(Long postId) {
        if (postId == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        postEntityRepository.findWithLockingByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST))
                .decrementCommentCount();
    }
}
