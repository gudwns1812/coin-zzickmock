package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import java.util.Optional;

public abstract class TestAuthProvider implements AuthProvider {
    @Override
    public Actor currentActor() {
        throw new UnsupportedOperationException("currentActor is not implemented for this test fake");
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public Optional<Actor> currentActorOptional() {
        return isAuthenticated() ? Optional.of(currentActor()) : Optional.empty();
    }
}
