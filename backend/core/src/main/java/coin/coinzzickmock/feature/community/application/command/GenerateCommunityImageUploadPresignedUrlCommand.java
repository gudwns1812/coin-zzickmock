package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record GenerateCommunityImageUploadPresignedUrlCommand(
        Long actorMemberId,
        String fileName,
        String contentType,
        long contentLength
) {
    public GenerateCommunityImageUploadPresignedUrlCommand {
        if (actorMemberId == null || actorMemberId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (fileName == null || fileName.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (contentType == null || contentType.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (contentLength <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        fileName = fileName.trim();
        contentType = contentType.trim();
    }
}
