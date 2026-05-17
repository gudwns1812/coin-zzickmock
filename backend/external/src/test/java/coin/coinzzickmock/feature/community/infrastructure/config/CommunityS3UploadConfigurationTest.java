package coin.coinzzickmock.feature.community.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import coin.coinzzickmock.feature.community.application.gateway.CommunityImageUploadGateway;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class CommunityS3UploadConfigurationTest {
    @Test
    void derivesPublicBaseUrlFromBucketAndRegionWhenCdnBaseUrlIsBlank() {
        CommunityImageUploadGateway gateway = new CommunityS3UploadConfiguration().communityImageUploadGateway(
                mock(S3Presigner.class),
                "coin-zzickmock-community-prod-672420933257-ap-southeast-2-an",
                "ap-southeast-2",
                " "
        );

        assertThat(gateway.getPublicUrl("community/7/image.webp"))
                .isEqualTo("https://coin-zzickmock-community-prod-672420933257-ap-southeast-2-an.s3.ap-southeast-2.amazonaws.com/community/7/image.webp");
    }
}
