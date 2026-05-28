package coin.coinzzickmock.providers.infrastructure;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
class PushJwtConfiguration {
    @Bean
    JwtDecoder pushAccessTokenJwtDecoder(@Value("${app.auth.jwt-secret}") String jwtSecret) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        MacAlgorithm algorithm = macAlgorithm(secretBytes);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(secretBytes, algorithm))
                .macAlgorithm(algorithm)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                accessTokenTypeValidator()
        ));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> accessTokenTypeValidator() {
        return token -> {
            if ("ACCESS".equals(token.getClaimAsString("tokenType"))) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "JWT tokenType must be ACCESS.", null));
        };
    }

    private MacAlgorithm macAlgorithm(byte[] secretBytes) {
        if (secretBytes.length >= 64) {
            return MacAlgorithm.HS512;
        }
        if (secretBytes.length >= 48) {
            return MacAlgorithm.HS384;
        }
        if (secretBytes.length >= 32) {
            return MacAlgorithm.HS256;
        }
        throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HMAC signing.");
    }

    private SecretKey secretKey(byte[] secretBytes, MacAlgorithm algorithm) {
        if (MacAlgorithm.HS512.equals(algorithm)) {
            return new SecretKeySpec(secretBytes, "HmacSHA512");
        }
        if (MacAlgorithm.HS384.equals(algorithm)) {
            return new SecretKeySpec(secretBytes, "HmacSHA384");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
