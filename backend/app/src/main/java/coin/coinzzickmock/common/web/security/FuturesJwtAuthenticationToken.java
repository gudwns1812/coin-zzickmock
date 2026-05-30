package coin.coinzzickmock.common.web.security;

import coin.coinzzickmock.providers.auth.Actor;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@Getter
public class FuturesJwtAuthenticationToken extends AbstractAuthenticationToken {
    private final Jwt credentials;
    private final Actor principal;

    public FuturesJwtAuthenticationToken(
            Jwt credentials,
            Actor principal,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.credentials = credentials;
        this.principal = principal;
        setAuthenticated(true);
    }
    
    @Override
    public String getName() {
        if (principal.account() != null && !principal.account().isBlank()) {
            return principal.account();
        }
        return String.valueOf(principal.memberId());
    }
}
