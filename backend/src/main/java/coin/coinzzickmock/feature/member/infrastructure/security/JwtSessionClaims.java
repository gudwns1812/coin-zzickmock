package coin.coinzzickmock.feature.member.infrastructure.security;

import coin.coinzzickmock.feature.member.domain.MemberRole;

public record JwtSessionClaims(
        String memberId,
        String memberName,
        String email,
        MemberRole role
) {
}
