package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostMutationResult;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
import coin.coinzzickmock.feature.community.domain.TiptapJsonImagePolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostImageRepository communityPostImageRepository;
    private final Clock clock;

    @Transactional
    public CommunityPostMutationResult execute(CreateCommunityPostCommand command) {
        CommunityCategory category = command.category();
        if (!CommunityPermissionPolicy.canCreatePost(command.actorAdmin(), category)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        validateImageOwnership(command.actorMemberId(), Set.copyOf(command.imageObjectKeys()));
        Instant now = Instant.now(clock);
        CommunityPost post = CommunityPost.create(command.actorMemberId(), command.authorNickname(), category,
                command.title(), validatedContent(command), now);
        CommunityPost saved = communityPostRepository.save(post);
        communityPostImageRepository.attachToPost(saved.id(), command.actorMemberId(), Set.copyOf(command.imageObjectKeys()), CommunityImageStatus.ATTACHED);
        return CommunityPostMutationResult.from(saved);
    }

    private TiptapJsonDocument validatedContent(CreateCommunityPostCommand command) {
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
