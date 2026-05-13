package coin.coinzzickmock.feature.community.application.query;

public record GetCommunityPostQuery(Long postId, Long actorMemberId, boolean actorAdmin) {
}
