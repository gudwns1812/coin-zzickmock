package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.command.UpdateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostResult;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityImageRepository communityImageRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final Clock clock;

    @Transactional
    public CommunityPostResult update(UpdateCommunityPostCommand command) {
        var post = communityPostRepository.findActiveByIdForUpdate(command.postId())
                .orElseThrow(CommunityApplicationSupport::notFound);
        boolean author = post.authorMemberId().equals(command.actorMemberId());
        if (!CommunityPermissionPolicy.canEditPost(command.actorAdmin(), author, post.category(), command.category())) {
            throw CommunityApplicationSupport.forbidden();
        }
        Instant now = Instant.now(clock);
        var content = CommunityApplicationSupport.content(command.actorMemberId(), command.contentJson(),
                command.imageObjectKeys(), command.allowedImageSrcPrefixes(), communityImageRepository);
        var updated = post.recategorize(command.category(), now)
                .rename(command.title(), now)
                .rewriteContent(content, now);
        var saved = communityPostRepository.save(updated);
        communityImageRepository.attachToPost(saved.id(), command.actorMemberId(), command.imageObjectKeys(), now);
        boolean liked = communityLikeRepository.exists(saved.id(), command.actorMemberId());
        return CommunityPostResult.from(saved, command.actorMemberId(), command.actorAdmin(), liked);
    }
}
