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
        ensurePostExists(command.postId());
        if (communityPostLikeRepository.addIfAbsent(command.postId(), command.actorMemberId())) {
            communityPostRepository.incrementLikeCount(command.postId());
        }
        return new CommunityLikeResult(command.postId(), true);
    }

    @Transactional
    public CommunityLikeResult unlike(ToggleCommunityPostLikeCommand command) {
        ensurePostExists(command.postId());
        if (communityPostLikeRepository.removeIfPresent(command.postId(), command.actorMemberId())) {
            communityPostRepository.decrementLikeCount(command.postId());
        }
        return new CommunityLikeResult(command.postId(), false);
    }

    private void ensurePostExists(Long postId) {
        communityPostRepository.findActiveById(postId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
    }
}
