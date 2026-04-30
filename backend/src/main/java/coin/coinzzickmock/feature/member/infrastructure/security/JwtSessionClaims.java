package coin.coinzzickmock.feature.member.infrastructure.security;

import coin.coinzzickmock.feature.member.domain.MemberRole;

public record JwtSessionClaims(
        Long memberId,
        String account,
        String nickname,
        String email,
        MemberRole role
) {
}
