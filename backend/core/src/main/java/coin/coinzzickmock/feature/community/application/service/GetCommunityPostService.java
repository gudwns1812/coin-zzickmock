package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.dto.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.application.query.GetCommunityPostQuery;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.view.CommunityPostReadIntent;
import coin.coinzzickmock.feature.community.application.view.CommunityPostViewThrottle;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCommunityPostService {
    private static final Duration VIEW_THROTTLE_WINDOW = Duration.ofMinutes(1);
    private static final Logger log = LoggerFactory.getLogger(GetCommunityPostService.class);

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostViewThrottle communityPostViewThrottle;

    public CommunityPostDetailResult execute(GetCommunityPostQuery query) {
        CommunityPost post = communityPostRepository.findActiveById(query.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        enforceReadIntent(query, post);
        recordViewIfNeeded(query);
        boolean isLikedByMe = query.actorMemberId() != null
                && communityPostLikeRepository.exists(query.postId(), query.actorMemberId());
        return CommunityPostDetailResult.from(post, query.actorMemberId(), query.isActorAdmin(), isLikedByMe);
    }

    private void enforceReadIntent(GetCommunityPostQuery query, CommunityPost post) {
        if (query.intent() != CommunityPostReadIntent.EDIT_PRELOAD) {
            return;
        }
        boolean isAuthor = Objects.equals(query.actorMemberId(), post.authorMemberId());
        boolean canEdit = CommunityPermissionPolicy.canEditPost(
                query.isActorAdmin(),
                isAuthor,
                post.category(),
                post.category()
        );
        if (!canEdit) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
    }

    private void recordViewIfNeeded(GetCommunityPostQuery query) {
        if (query.intent() != CommunityPostReadIntent.DETAIL || query.actorMemberId() == null) {
            return;
        }
        if (!claimView(query)) {
            return;
        }
        try {
            communityPostRepository.incrementViewCount(query.postId());
        } catch (RuntimeException exception) {
            log.warn(Skipped
