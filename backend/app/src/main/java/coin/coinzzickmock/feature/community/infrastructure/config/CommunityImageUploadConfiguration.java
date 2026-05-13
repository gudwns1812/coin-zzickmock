package coin.coinzzickmock.feature.community.infrastructure.config;

import coin.coinzzickmock.feature.community.application.CommunityImageUploadSettings;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityImageUploadConfiguration {
    @Bean
    CommunityImageUploadSettings communityImageUploadSettings(
            @Value("${coin.community.images.allowed-mime:image/png,image/jpeg,image/webp,image/gif}") String allowedMime,
            @Value("${coin.community.images.max-bytes:5242880}") long maxBytes,
            @Value("${coin.community.images.presign-ttl:PT10M}") Duration presignTtl,
            @Value("${coin.community.images.key-prefix:community}") String keyPrefix
    ) {
        Set<String> allowedContentTypes = Arrays.stream(allowedMime.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        return new CommunityImageUploadSettings(allowedContentTypes, maxBytes, presignTtl, keyPrefix);
    }
}
