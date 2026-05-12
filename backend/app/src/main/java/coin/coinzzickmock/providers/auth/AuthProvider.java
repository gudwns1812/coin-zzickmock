package coin.coinzzickmock.providers.auth;

import java.util.Optional;

public interface AuthProvider {
    Actor currentActor();

    boolean isAuthenticated();

    Optional<Actor> currentActorOptional();
}
