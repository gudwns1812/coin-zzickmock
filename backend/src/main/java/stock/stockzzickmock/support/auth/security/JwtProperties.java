package stock.stockzzickmock.support.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpirationSeconds,
        long refreshExpirationSeconds,
        boolean cookieSecure
) {
}
