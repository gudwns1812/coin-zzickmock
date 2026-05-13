package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import java.util.List;

public record CreateCommunityPostCommand(
        Long actorMemberId,
        boolean actorAdmin,
        String authorNickname,
        CommunityCategory category,
        String title,
        String contentJson,
        List<String> imageObjectKeys,
        List<String> allowedImageSrcPrefixes
) {
    public CreateCommunityPostCommand {
        imageObjectKeys = imageObjectKeys == null ? List.of() : List.copyOf(imageObjectKeys);
        allowedImageSrcPrefixes = allowedImageSrcPrefixes == null ? List.of() : List.copyOf(allowedImageSrcPrefixes);
    }
}
