package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.util.List;

public record ValidateCommunityImagesCommand(Long uploaderMemberId, List<String> objectKeys) {
    public ValidateCommunityImagesCommand {
        if (uploaderMemberId == null || uploaderMemberId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        objectKeys = objectKeys == null ? List.of() : List.copyOf(objectKeys);
    }
}
