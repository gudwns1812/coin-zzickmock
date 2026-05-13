package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.query.CommunityPostListQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import coin.coinzzickmock.feature.community.application.result.CommunityPostSummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCommunityPostsService {
    private static final int NOTICE_LIMIT = 3;

    private final CommunityPostRepository communityPostRepository;

    @Transactional(readOnly = true)
    public CommunityPostListResult list(CommunityPostListQuery query) {
        var notices = communityPostRepository.findLatestNotices(NOTICE_LIMIT).stream()
                .map(CommunityPostSummaryResult::from)
                .toList();
        var page = communityPostRepository.findNormalPosts(query);
        return new CommunityPostListResult(
                notices,
                page.posts().stream().map(CommunityPostSummaryResult::from).toList(),
                query.page(),
                query.size(),
                page.hasNext()
        );
    }
}
