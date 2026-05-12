package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberRole;

public record AuthUserResponse(
        Long memberId,
        String account,
        String memberName,
        String nickname,
        MemberRole role,
        boolean admin
) {
    public static AuthUserResponse from(MemberProfileResult result) {
        return new AuthUserResponse(
                result.memberId(),
                result.account(),
                result.memberName(),
                result.nickname(),
                result.role(),
                result.role() == MemberRole.ADMIN
        );
    }
}
