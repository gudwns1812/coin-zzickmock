package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.content.TiptapContentPolicy;
import java.util.Set;

public record CreateCommunityPostCommand(
        Long actorMemberId,
        String actorNickname,
        boolean actorAdmin,
        CommunityCategory category,
        String title,
        String contentJson,
        Set<String> imageObjectKeys,
        TiptapContentPolicy contentPolicy
) {
    public CreateCommunityPostCommand {
        imageObjectKeys = imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}
