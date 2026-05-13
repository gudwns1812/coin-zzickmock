package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.command.UpdateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityLikeRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostResult;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
import coin.coinzzickmock.feature.community.domain.TiptapJsonImagePolicy;
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
        validateImageOwnership(command.actorMemberId(), Set.copyOf(command.imageObjectKeys()));
        Instant now = Instant.now(clock);
        var content = CommunityApplicationSupport.content(command.actorMemberId(), command.contentJson(),
                command.imageObjectKeys(), command.allowedImageSrcPrefixes(), communityImageRepository);
        var updated = post.recategorize(command.category(), now)
                .rename(command.title(), now)
                .rewriteContent(validatedContent(command), now);
        CommunityPost saved = communityPostRepository.update(updated);
        communityPostImageRepository.attachToPost(saved.id(), command.actorMemberId(), Set.copyOf(command.imageObjectKeys()), CommunityImageStatus.ATTACHED);
        communityPostImageRepository.detachMissingImages(saved.id(), Set.copyOf(command.imageObjectKeys()), CommunityImageStatus.ORPHANED);
        return CommunityPostMutationResult.from(saved);
    }


    private TiptapJsonDocument validatedContent(UpdateCommunityPostCommand command) {
        if (command.imageObjectKeys().isEmpty()) {
            return TiptapJsonDocument.of(command.contentJson());
        }
        return TiptapJsonDocument.of(
                command.contentJson(),
                new TiptapJsonImagePolicy("community/" + command.actorMemberId() + "/", command.allowedImageSrcPrefixes())
        );
    }

    private void validateImageOwnership(Long memberId, Set<String> objectKeys) {
        Set<String> attachable = communityPostImageRepository.findAttachableObjectKeys(memberId, objectKeys);
        if (!attachable.containsAll(objectKeys)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
