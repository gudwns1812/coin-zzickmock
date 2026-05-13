package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.ToggleCommunityPostLikeCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityLikeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleCommunityPostLikeService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityLikeRepository communityLikeRepository;

    @Transactional
    public CommunityLikeResult like(ToggleCommunityPostLikeCommand command) {
        ensurePostExists(command.postId());
        CommunityLikeResult result = communityLikeRepository.addIfAbsent(command.postId(), command.actorMemberId());
        if (result.likedByMe()) {
            communityPostRepository.findActiveByIdForUpdate(command.postId())
                    .ifPresent(post -> communityPostRepository.save(post.incrementLikeCount(java.time.Instant.now())));
        }
        return result;
    }

    @Transactional
    public CommunityLikeResult unlike(ToggleCommunityPostLikeCommand command) {
        ensurePostExists(command.postId());
        CommunityLikeResult result = communityLikeRepository.removeIfPresent(command.postId(), command.actorMemberId());
        if (!result.likedByMe()) {
            communityPostRepository.findActiveByIdForUpdate(command.postId())
                    .filter(post -> post.likeCount() > result.likeCount())
                    .ifPresent(post -> communityPostRepository.save(post.decrementLikeCount(java.time.Instant.now())));
        }
        return result;
    }

    private void ensurePostExists(Long postId) {
        communityPostRepository.findActiveById(postId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
    }
}
