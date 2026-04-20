package coin.coinzzickmock.feature.member.infrastructure.security;

public record JwtSessionClaims(
        String memberId,
        String memberName,
        String email
) {
}
