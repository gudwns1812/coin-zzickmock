package coin.coinzzickmock.providers.auth;

import coin.coinzzickmock.feature.member.domain.MemberRole;

public record Actor(
        String memberId,
        String email,
        String nickname,
        MemberRole role
) {
    public Actor(String memberId, String email, String nickname) {
        this(memberId, email, nickname, MemberRole.USER);
    }

    public boolean admin() {
        return role == MemberRole.ADMIN;
    }
}
