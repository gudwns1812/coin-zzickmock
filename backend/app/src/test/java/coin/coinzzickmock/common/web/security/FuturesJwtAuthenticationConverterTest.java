package coin.coinzzickmock.common.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import coin.coinzzickmock.providers.auth.ActorRole;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class FuturesJwtAuthenticationConverterTest {
    @Test
    void createsPrincipalAndAuthoritiesFromDbBackedActor() {
        Actor adminActor = new Actor(1L, "admin", "admin@example.com", "Admin", ActorRole.ADMIN);
        FuturesJwtAuthenticationConverter converter = new FuturesJwtAuthenticationConverter(new FixedActorLookup(adminActor));
        Jwt jwt = jwt("1", "USER");

        FuturesJwtAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isEqualTo(adminActor);
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void rejectsStaleMemberId() {
        FuturesJwtAuthenticationConverter converter = new FuturesJwtAuthenticationConverter(new FixedActorLookup(null));

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt("1", "ADMIN")));
    }

    @Test
    void rejectsUnexpectedSubject() {
        FuturesJwtAuthenticationConverter converter = new FuturesJwtAuthenticationConverter(new FixedActorLookup(
                new Actor(1L, "demo", "demo@example.com", "Demo")
        ));

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt("not-a-member-id", "ADMIN")));
    }

    private Jwt jwt(String subject, String roleClaim) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", subject, "tokenType", "ACCESS", "role", roleClaim)
        );
    }

    private record FixedActorLookup(Actor actor) implements ActorLookup {
        @Override
        public Optional<Actor> findByMemberId(Long memberId) {
            return Optional.ofNullable(actor);
        }

        @Override
        public Optional<Actor> findByAccount(String account) {
            throw new UnsupportedOperationException("findByAccount should not be called by JWT authentication conversion.");
        }
    }
}
