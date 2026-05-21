package coin.coinzzickmock.providers.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AccessTokenTypeValidatorTest {
    private final AccessTokenTypeValidator validator = new AccessTokenTypeValidator();

    @Test
    void acceptsAccessTokenType() {
        assertThat(validator.validate(jwt("ACCESS")).hasErrors()).isFalse();
    }

    @Test
    void rejectsMissingOrUnexpectedTokenType() {
        assertThat(validator.validate(jwt(null)).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("REFRESH")).hasErrors()).isTrue();
    }

    private Jwt jwt(String tokenType) {
        Map<String, Object> claims = tokenType == null ? Map.of("sub", "1") : Map.of("sub", "1", "tokenType", tokenType);
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }
}
