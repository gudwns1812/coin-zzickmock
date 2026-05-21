package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAuthProvider implements AuthProvider {
    @Override
    public Actor currentActor() {
        return currentActorOptional()
                .orElseThrow(() -> new CoreException(ErrorCode.UNAUTHORIZED));
    }

    @Override
    public boolean isAuthenticated() {
        return currentActorOptional().isPresent();
    }

    @Override
    public Optional<Actor> currentActorOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Actor actor ? Optional.of(actor) : Optional.empty();
    }
}
