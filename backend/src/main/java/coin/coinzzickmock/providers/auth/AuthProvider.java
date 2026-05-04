package coin.coinzzickmock.providers.auth;

import java.util.Optional;

public interface AuthProvider {
    Actor currentActor();

    boolean isAuthenticated();

    default Optional<Actor> currentActorOptional() {
        return isAuthenticated() ? Optional.of(currentActor()) : Optional.empty();
    }
}
