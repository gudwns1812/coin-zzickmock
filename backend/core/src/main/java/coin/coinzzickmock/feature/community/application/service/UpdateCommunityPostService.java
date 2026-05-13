package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.UpdateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostMutationResult;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostImageRepository communityPostImageRepository;
    private final Clock clock;

    @Transactional
    public CommunityPostMutationResult execute(UpdateCommunityPostCommand command) {
        CommunityPost existing = communityPostRepository.findActiveById(command.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        boolean author = existing.authorMemberId().equals(command.actorMemberId());
        if (!CommunityPermissionPolicy.canEditPost(command.actorAdmin(), author, existing.category(), command.category())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        validateImageOwnership(command.actorMemberId(), command.imageObjectKeys());
        Instant now = Instant.now(clock);
        CommunityPost updated = existing.recategorize(command.category(), now)
                .rename(command.title(), now)
                .rewriteContent(TiptapJsonDocument.of(command.contentJson()), now);
        CommunityPost saved = communityPostRepository.update(updated);
        communityPostImageRepository.attachToPost(saved.id(), command.actorMemberId(), command.imageObjectKeys(), CommunityImageStatus.ATTACHED);
        communityPostImageRepository.detachMissingImages(saved.id(), command.imageObjectKeys(), CommunityImageStatus.ORPHANED);
        return CommunityPostMutationResult.from(saved);
    }

    private void validateImageOwnership(Long memberId, Set<String> objectKeys) {
        Set<String> attachable = communityPostImageRepository.findAttachableObjectKeys(memberId, objectKeys);
        if (!attachable.containsAll(objectKeys)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
