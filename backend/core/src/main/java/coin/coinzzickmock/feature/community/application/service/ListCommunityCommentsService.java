package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.query.ListCommunityCommentsQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentListResult;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCommunityCommentsService {
    private final CommunityCommentRepository communityCommentRepository;

    @Transactional(readOnly = true)
    public CommunityCommentListResult execute(ListCommunityCommentsQuery query, Long actorMemberId, boolean actorAdmin) {
        var page = communityCommentRepository.findActiveByPostId(query.postId(), query.page(), query.size());
        return new CommunityCommentListResult(
                page.comments().stream()
                        .map(comment -> CommunityCommentResult.from(comment, actorMemberId, actorAdmin))
                        .toList(),
                query.page(),
                query.size(),
                page.hasNext()
        );
    }
}
