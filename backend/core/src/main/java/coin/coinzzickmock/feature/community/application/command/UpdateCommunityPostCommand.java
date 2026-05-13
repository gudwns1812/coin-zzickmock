package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.content.TiptapContentPolicy;
import java.util.Set;

public record UpdateCommunityPostCommand(
        Long postId,
        Long actorMemberId,
        boolean isActorAdmin,
        CommunityCategory category,
        String title,
        String contentJson,
        Set<String> imageObjectKeys,
        TiptapContentPolicy contentPolicy
) {
    public UpdateCommunityPostCommand {
        imageObjectKeys = imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}
