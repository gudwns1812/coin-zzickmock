package coin.coinzzickmock.feature.community.infrastructure.persistence;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommunityPostRepositorySupport {
    private final CommunityPostEntityRepository postEntityRepository;

    void decrementCommentCount(Long postId) {
        postEntityRepository.findWithLockingByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST))
                .decrementCommentCount();
    }
}
