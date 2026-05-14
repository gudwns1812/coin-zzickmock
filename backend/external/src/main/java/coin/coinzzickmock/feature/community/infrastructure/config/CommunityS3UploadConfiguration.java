package coin.coinzzickmock.feature.community.infrastructure.config;

import coin.coinzzickmock.feature.community.application.gateway.CommunityImageUploadGateway;
import coin.coinzzickmock.feature.community.infrastructure.S3CommunityImageUploadGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class CommunityS3UploadConfiguration {
    @Bean(destroyMethod = "close")
    S3Presigner communityS3Presigner(
            @Value("${coin.community.s3.region:ap-northeast-2}") String region
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    CommunityImageUploadGateway communityImageUploadGateway(
            S3Presigner communityS3Presigner,
            @Value("${coin.community.s3.bucket:coin-zzickmock-community-local}") String bucket,
            @Value("${coin.community.s3.public-base-url:https://coin-zzickmock-community-local.s3.ap-northeast-2.amazonaws.com}") String publicBaseUrl
    ) {
        return new S3CommunityImageUploadGateway(communityS3Presigner, bucket, publicBaseUrl);
    }
}
