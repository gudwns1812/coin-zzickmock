package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.ToggleCommunityPostLikeCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityLikeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleCommunityPostLikeService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;

    @Transactional
    public CommunityLikeResult like(ToggleCommunityPostLikeCommand command) {
        long likeCount = currentLikeCount(command.postId());
        boolean inserted = communityPostLikeRepository.addIfAbsent(command.postId(), command.actorMemberId());
        if (inserted) {
            likeCount = communityPostRepository.incrementLikeCount(command.postId());
        }
        return new CommunityLikeResult(command.postId(), true, likeCount);
    }

    @Transactional
    public CommunityLikeResult unlike(ToggleCommunityPostLikeCommand command) {
        long likeCount = currentLikeCount(command.postId());
        boolean removed = communityPostLikeRepository.removeIfPresent(command.postId(), command.actorMemberId());
        if (removed) {
            likeCount = communityPostRepository.decrementLikeCount(command.postId());
        }
        return new CommunityLikeResult(command.postId(), false, likeCount);
    }

    private long currentLikeCount(Long postId) {
        return communityPostRepository.findActiveById(postId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST))
                .likeCount();
    }
}
