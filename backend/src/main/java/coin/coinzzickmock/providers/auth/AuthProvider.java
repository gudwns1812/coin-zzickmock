package coin.coinzzickmock.providers.auth;

public interface AuthProvider {
    Actor currentActor();

    boolean isAuthenticated();
}
