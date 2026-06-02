package coin.coinzzickmock.feature.community.application.dto;

public record GenerateCommunityImageUploadPresignedUrlCommand(
        Long actorMemberId,
        String fileName,
        String contentType,
        long contentLength
) {
    public GenerateCommunityImageUploadPresignedUrlCommand {
        fileName = fileName.trim();
        contentType = contentType.trim();
    }
}
