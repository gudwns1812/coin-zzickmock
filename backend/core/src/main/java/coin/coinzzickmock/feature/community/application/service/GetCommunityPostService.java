package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.query.GetCommunityPostQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final Clock clock;

    @Transactional
    public CommunityPostDetailResult execute(GetCommunityPostQuery query) {
        CommunityPost post = communityPostRepository.findActiveById(query.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        communityPostRepository.incrementViewCount(query.postId());
        CommunityPost viewedPost = post.incrementViewCount(Instant.now(clock));
        boolean isLikedByMe = query.actorMemberId() != null
                && communityPostLikeRepository.exists(query.postId(), query.actorMemberId());
        return CommunityPostDetailResult.from(viewedPost, query.actorMemberId(), query.isActorAdmin(), isLikedByMe);
    }
}
