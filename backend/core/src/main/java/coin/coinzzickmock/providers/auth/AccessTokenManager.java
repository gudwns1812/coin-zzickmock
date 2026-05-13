package coin.coinzzickmock.providers.auth;

public interface AccessTokenManager {
    String issue(AuthSessionClaims claims);

    AuthSessionClaims parse(String token);

    String accessTokenCookieName();

    long accessTokenExpirationSeconds();

    boolean secureCookie();
}
