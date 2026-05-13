package coin.coinzzickmock.feature.community.application.command;

public record DeleteCommunityCommentCommand(Long commentId, Long actorMemberId, boolean actorAdmin) {
}
