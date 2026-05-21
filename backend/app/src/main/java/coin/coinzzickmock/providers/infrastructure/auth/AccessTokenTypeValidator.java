package coin.coinzzickmock.providers.infrastructure.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenTypeValidator implements OAuth2TokenValidator<Jwt> {
    static final String ACCESS_TOKEN_TYPE = "ACCESS";
    static final String TOKEN_TYPE_CLAIM = "tokenType";

    private static final OAuth2Error INVALID_TOKEN_TYPE = new OAuth2Error(
            "invalid_token",
            "JWT tokenType must be ACCESS.",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (ACCESS_TOKEN_TYPE.equals(token.getClaimAsString(TOKEN_TYPE_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN_TYPE);
    }
}
