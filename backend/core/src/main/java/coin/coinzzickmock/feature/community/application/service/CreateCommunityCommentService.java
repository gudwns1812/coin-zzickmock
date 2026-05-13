package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.command.CreateCommunityCommentCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityCommentRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityCommentMutationResult;
import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommunityCommentService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final Clock clock;

    @Transactional
    public CommunityCommentMutationResult execute(CreateCommunityCommentCommand command) {
        communityPostRepository.findActiveById(command.postId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        CommunityComment comment = CommunityComment.create(command.postId(), command.actorMemberId(), command.authorNickname(), command.content(), Instant.now(clock));
        CommunityComment saved = communityCommentRepository.save(comment);
        communityPostRepository.incrementCommentCount(command.postId());
        return CommunityCommentMutationResult.from(saved);
    }
}
