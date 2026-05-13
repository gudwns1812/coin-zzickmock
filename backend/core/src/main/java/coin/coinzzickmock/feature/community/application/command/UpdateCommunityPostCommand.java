package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import java.util.List;

public record UpdateCommunityPostCommand(
        Long postId,
        Long actorMemberId,
        boolean actorAdmin,
        CommunityCategory category,
        String title,
        String contentJson,
        List<String> imageObjectKeys,
        List<String> allowedImageSrcPrefixes
) {
    public UpdateCommunityPostCommand {
        imageObjectKeys = imageObjectKeys == null ? List.of() : List.copyOf(imageObjectKeys);
        allowedImageSrcPrefixes = allowedImageSrcPrefixes == null ? List.of() : List.copyOf(allowedImageSrcPrefixes);
    }
}
