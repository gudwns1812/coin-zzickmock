package coin.coinzzickmock.feature.community.application.command;

public record CreateCommunityCommentCommand(Long postId, Long actorMemberId, String authorNickname, String content) {
}
