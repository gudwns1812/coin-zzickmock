package coin.coinzzickmock.feature.community.application.command;

public record DeleteCommunityPostCommand(Long postId, Long actorMemberId, boolean actorAdmin) {
}
