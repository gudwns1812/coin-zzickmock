package coin.coinzzickmock.feature.member.api;

public record LoginRequest(
        String account,
        String password
) {
}
