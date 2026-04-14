package coin.coinzzickmock.support.auth.application.result;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
