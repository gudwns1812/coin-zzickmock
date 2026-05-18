package coin.coinzzickmock.providers.infrastructure.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtAccessTokenConfiguration {
    private static final String TEST_DEFAULT_JWT_SECRET = "coin-zzickmock-test-only-secret-please-change";

    private final String jwtSecret;
    private final Environment environment;

    public JwtAccessTokenConfiguration(
            @Value("${app.auth.jwt-secret:}") String jwtSecret,
            Environment environment
    ) {
        this.jwtSecret = jwtSecret;
        this.environment = environment;
    }

    @Bean
    JwtAccessTokenSigningMaterial accessTokenSigningMaterial() {
        byte[] secretBytes = resolveJwtSecret().getBytes(StandardCharsets.UTF_8);
        MacAlgorithm macAlgorithm = macAlgorithm(secretBytes);
        return new JwtAccessTokenSigningMaterial(secretKey(secretBytes, macAlgorithm), macAlgorithm);
    }

    @Bean
    JwtEncoder accessTokenJwtEncoder(JwtAccessTokenSigningMaterial signingMaterial) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingMaterial.secretKey()));
    }

    @Bean
    JwtDecoder accessTokenJwtDecoder(JwtAccessTokenSigningMaterial signingMaterial) {
        return NimbusJwtDecoder.withSecretKey(signingMaterial.secretKey())
                .macAlgorithm(signingMaterial.macAlgorithm())
                .build();
    }

    @Bean
    JwsHeader accessTokenJwsHeader(JwtAccessTokenSigningMaterial signingMaterial) {
        return JwsHeader.with(signingMaterial.macAlgorithm()).build();
    }

    private String resolveJwtSecret() {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return jwtSecret;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equals(profile)) {
                return TEST_DEFAULT_JWT_SECRET;
            }
        }
        throw new IllegalStateException("JWT_SECRET must be configured outside test profile.");
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

    private SecretKey secretKey(byte[] secretBytes, MacAlgorithm macAlgorithm) {
        return new SecretKeySpec(secretBytes, hmacAlgorithm(macAlgorithm));
    }

    private String hmacAlgorithm(MacAlgorithm macAlgorithm) {
        if (MacAlgorithm.HS512.equals(macAlgorithm)) {
            return "HmacSHA512";
        }
        if (MacAlgorithm.HS384.equals(macAlgorithm)) {
            return "HmacSHA384";
        }
        return "HmacSHA256";
    }

    record JwtAccessTokenSigningMaterial(SecretKey secretKey, MacAlgorithm macAlgorithm) {
    }
}
