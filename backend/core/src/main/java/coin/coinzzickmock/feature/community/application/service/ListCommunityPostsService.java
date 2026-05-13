package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.query.ListCommunityPostsQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCommunityPostsService {
    private static final int NOTICE_LIMIT = 3;

    private final CommunityPostRepository communityPostRepository;

    @Transactional(readOnly = true)
    public CommunityPostListResult execute(ListCommunityPostsQuery query) {
        List<CommunityPost> notices = communityPostRepository.findLatestNotices(NOTICE_LIMIT);
        CommunityPostPage posts = communityPostRepository.findPosts(query);
        return CommunityPostListResult.from(notices, posts);
    }
}
