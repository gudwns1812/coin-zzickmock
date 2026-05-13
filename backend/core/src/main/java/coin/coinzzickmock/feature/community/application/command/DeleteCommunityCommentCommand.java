package coin.coinzzickmock.feature.community.application.command;

public record DeleteCommunityCommentCommand(Long postId, Long commentId, Long actorMemberId, boolean isActorAdmin) {
}
