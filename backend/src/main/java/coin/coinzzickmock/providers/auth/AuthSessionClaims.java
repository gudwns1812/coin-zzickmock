package coin.coinzzickmock.providers.auth;

public record AuthSessionClaims(
        Long memberId,
        String account,
        String nickname,
        String email,
        ActorRole role
) {
}
