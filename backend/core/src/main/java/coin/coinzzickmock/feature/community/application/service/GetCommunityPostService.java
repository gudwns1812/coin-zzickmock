package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.query.GetCommunityPostQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;

    @Transactional(readOnly = true)
    public CommunityPostDetailResult execute(GetCommunityPostQuery query) {
        CommunityPost post = communityPostRepository.findActiveById(query.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        boolean liked = communityPostLikeRepository.exists(query.postId(), query.actorMemberId());
        return CommunityPostDetailResult.from(post, query.actorMemberId(), query.actorAdmin(), liked);
    }
}
