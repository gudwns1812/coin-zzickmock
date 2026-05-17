package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.result.CommunityImageUploadPresignedUrlResult;
import java.time.Instant;

public record CommunityImageUploadPresignedUrlResponse(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        String contentType,
        Instant expiresAt,
        long maxBytes
) {
    public static CommunityImageUploadPresignedUrlResponse from(CommunityImageUploadPresignedUrlResult result) {
        return new CommunityImageUploadPresignedUrlResponse(
                result.uploadUrl(),
                result.objectKey(),
                result.publicUrl(),
                result.contentType(),
                result.expiresAt(),
                result.maxBytes()
        );
    }
}
