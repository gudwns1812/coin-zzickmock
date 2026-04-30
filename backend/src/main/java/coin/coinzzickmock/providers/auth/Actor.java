package coin.coinzzickmock.providers.auth;

import coin.coinzzickmock.feature.member.domain.MemberRole;

public record Actor(
        Long memberId,
        String account,
        String email,
        String nickname,
        MemberRole role
) {
    public Actor(Long memberId, String account, String email, String nickname) {
        this(memberId, account, email, nickname, MemberRole.USER);
    }

    public boolean admin() {
        return role == MemberRole.ADMIN;
    }
}
