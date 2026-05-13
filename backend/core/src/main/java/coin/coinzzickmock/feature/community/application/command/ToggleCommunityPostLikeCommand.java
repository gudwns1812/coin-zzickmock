package coin.coinzzickmock.feature.community.application.command;

public record ToggleCommunityPostLikeCommand(Long postId, Long actorMemberId, boolean liked) {
}
