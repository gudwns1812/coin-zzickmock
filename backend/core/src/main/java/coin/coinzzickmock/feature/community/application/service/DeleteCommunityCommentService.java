package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.DeleteCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentRepository;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCommunityCommentService {
    private final CommunityCommentRepository communityCommentRepository;
    private final Clock clock;

    @Transactional
    public void execute(DeleteCommunityCommentCommand command) {
        CommunityComment comment = communityCommentRepository.findActiveById(command.commentId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        if (!CommunityPermissionPolicy.canDeleteComment(command.actorAdmin(), comment.authorMemberId().equals(command.actorMemberId()))) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        communityCommentRepository.softDelete(comment.id(), Instant.now(clock));
    }
}
