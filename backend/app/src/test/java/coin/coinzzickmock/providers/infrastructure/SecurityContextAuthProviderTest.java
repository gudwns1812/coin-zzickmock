package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextAuthProviderTest {
    private final SecurityContextAuthProvider provider = new SecurityContextAuthProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentActorReturnsActorPrincipalFromSecurityContext() {
        Actor actor = new Actor(1L, "demo", "demo@example.com", "Demo", ActorRole.ADMIN);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(actor, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Actor currentActor = provider.currentActor();

        assertThat(currentActor).isEqualTo(actor);
        assertThat(provider.isAuthenticated()).isTrue();
    }

    @Test
    void currentActorRejectsMissingAuthentication() {
        CoreException thrown = assertThrows(CoreException.class, provider::currentActor);

        assertEquals(ErrorCode.UNAUTHORIZED, thrown.errorCode());
        assertThat(provider.currentActorOptional()).isEmpty();
        assertThat(provider.isAuthenticated()).isFalse();
    }

    @Test
    void currentActorOptionalIgnoresNonActorPrincipal() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("demo", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(provider.currentActorOptional()).isEmpty();
        assertThat(provider.isAuthenticated()).isFalse();
    }
}
