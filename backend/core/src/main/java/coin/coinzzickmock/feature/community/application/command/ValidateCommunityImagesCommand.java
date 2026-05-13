package coin.coinzzickmock.feature.community.application.command;

import java.util.List;

public record ValidateCommunityImagesCommand(Long uploaderMemberId, List<String> objectKeys) {
    public ValidateCommunityImagesCommand {
        objectKeys = objectKeys == null ? List.of() : List.copyOf(objectKeys);
    }
}
