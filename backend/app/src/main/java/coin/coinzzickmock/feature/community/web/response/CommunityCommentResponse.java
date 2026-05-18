package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.dto.CommunityCommentResult;
import java.time.Instant;

public record CommunityCommentResponse(
        Long id,
        Long postId,
        String authorNickname,
        String content,
        boolean canDelete,
        Instant createdAt
) {
    public static CommunityCommentResponse from(CommunityCommentResult result) {
        return new CommunityCommentResponse(
                result.id(),
                result.postId(),
                result.authorNickname(),
                result.content(),
                result.canDelete(),
                result.createdAt()
        );
    }
}
