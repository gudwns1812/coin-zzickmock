package coin.coinzzickmock.feature.community.application.dto;

public record DeleteCommunityCommentCommand(Long postId, Long commentId, Long actorMemberId, boolean isActorAdmin) {
}
