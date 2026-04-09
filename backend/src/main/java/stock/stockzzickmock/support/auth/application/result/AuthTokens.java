package stock.stockzzickmock.support.auth.application.result;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
