package coin.coinzzickmock.providers.auth;

public interface AccessTokenManager {
    String issue(AuthSessionClaims claims);

    String accessTokenCookieName();

    long accessTokenExpirationSeconds();

    boolean secureCookie();
}
