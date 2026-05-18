package coin.coinzzickmock.feature.community.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import coin.coinzzickmock.common.error.CoreException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.junit.jupiter.api.Test;

class S3CommunityImageUploadGatewayTest {
    @Test
    void returnsPublicUrlWithoutLeakingPresignedUrl() {
        S3CommunityImageUploadGateway gateway = new S3CommunityImageUploadGateway(
                mock(S3Presigner.class),
                "bucket",
                "https://cdn.example.com/base/"
        );

        assertThat(gateway.getPublicUrl("community/1/image.webp"))
                .isEqualTo("https://cdn.example.com/base/community/1/image.webp");
    }

    @Test
    void rejectsUnsafeObjectKeys() {
        S3CommunityImageUploadGateway gateway = new S3CommunityImageUploadGateway(
                mock(S3Presigner.class),
                "bucket",
                "https://cdn.example.com"
        );

        assertThatThrownBy(() -> gateway.getPublicUrl("../secret"))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> gateway.getPublicUrl("/community/1/image.webp"))
                .isInstanceOf(CoreException.class);
    }
}
