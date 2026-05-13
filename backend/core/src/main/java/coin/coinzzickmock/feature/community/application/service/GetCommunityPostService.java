package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.repository.CommunityLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityLikeRepository communityLikeRepository;

    @Transactional(readOnly = true)
    public CommunityPostResult get(Long postId, Long viewerMemberId, boolean viewerAdmin) {
        var post = communityPostRepository.findActiveById(postId)
                .orElseThrow(CommunityApplicationSupport::notFound);
        boolean liked = viewerMemberId != null && communityLikeRepository.exists(postId, viewerMemberId);
        return CommunityPostResult.from(post, viewerMemberId, viewerAdmin, liked);
    }
}
