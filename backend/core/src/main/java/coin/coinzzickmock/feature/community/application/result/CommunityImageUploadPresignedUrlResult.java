package coin.coinzzickmock.feature.community.application.result;

import java.time.Instant;

public record CommunityImageUploadPresignedUrlResult(
        String objectKey,
        String uploadUrl,
        String publicUrl,
        String contentType,
        Instant expiresAt,
        long maxBytes
) {
}
