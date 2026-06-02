package coin.coinzzickmock.feature.community.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CommunityImageUploadPresignRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {
}
