package coin.coinzzickmock.feature.member.api;

import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberRole;

public record AuthUserResponse(
        String memberId,
        String memberName,
        MemberRole role,
        boolean admin
) {
    public static AuthUserResponse from(MemberProfileResult result) {
        return new AuthUserResponse(
                result.memberId(),
                result.memberName(),
                result.role(),
                result.role() == MemberRole.ADMIN
        );
    }
}
