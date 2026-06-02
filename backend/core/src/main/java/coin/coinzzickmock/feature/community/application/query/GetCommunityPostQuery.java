package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.feature.community.application.view.CommunityPostReadIntent;

public record GetCommunityPostQuery(Long postId, Long actorMemberId, boolean isActorAdmin, CommunityPostReadIntent intent) {
    public GetCommunityPostQuery(Long postId, Long actorMemberId, boolean isActorAdmin) {
        this(postId, actorMemberId, isActorAdmin, CommunityPostReadIntent.DETAIL);
    }

}
