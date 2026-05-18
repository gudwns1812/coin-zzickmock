package coin.coinzzickmock.feature.community.application.dto;

public record CreateCommunityCommentCommand(
        Long postId,
        Long actorMemberId,
        String actorNickname,
        String content
) {
}
