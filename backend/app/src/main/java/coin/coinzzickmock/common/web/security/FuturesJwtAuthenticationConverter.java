package coin.coinzzickmock.common.web.security;

import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FuturesJwtAuthenticationConverter implements Converter<Jwt, FuturesJwtAuthenticationToken> {
    private final ActorLookup actorLookup;

    @Override
    public FuturesJwtAuthenticationToken convert(Jwt jwt) {
        Long memberId = memberId(jwt);
        Actor actor = actorLookup.findByMemberId(memberId)
                .orElseThrow(() -> new BadCredentialsException("Active member not found."));
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + actor.role().name()));
        return new FuturesJwtAuthenticationToken(jwt, actor, authorities);
    }

    private Long memberId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("JWT subject is required.");
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("JWT subject must be a member id.", exception);
        }
    }
}
