package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final Clock clock;

    @Transactional
    public void execute(DeleteCommunityPostCommand command) {
        CommunityPost post = communityPostRepository.findActiveById(command.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        if (!CommunityPermissionPolicy.canDeletePost(command.actorAdmin(), post.authorMemberId().equals(command.actorMemberId()))) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        communityPostRepository.softDelete(command.postId(), Instant.now(clock));
    }
}
