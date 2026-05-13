package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.command.DeleteCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
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
    public void delete(DeleteCommunityPostCommand command) {
        var post = communityPostRepository.findActiveByIdForUpdate(command.postId())
                .orElseThrow(CommunityApplicationSupport::notFound);
        if (!CommunityPermissionPolicy.canDeletePost(command.actorAdmin(), post.authorMemberId().equals(command.actorMemberId()))) {
            throw CommunityApplicationSupport.forbidden();
        }
        communityPostRepository.save(post.softDelete(Instant.now(clock)));
    }
}
