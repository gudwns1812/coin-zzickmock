package coin.coinzzickmock.feature.community.application.dto;

public record DeleteCommunityPostCommand(Long postId, Long actorMemberId, boolean isActorAdmin) {
}
