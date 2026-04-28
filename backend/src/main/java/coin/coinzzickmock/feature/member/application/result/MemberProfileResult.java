package coin.coinzzickmock.feature.member.application.result;

import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberRole;

public record MemberProfileResult(
        String memberId,
        String memberName,
        String memberEmail,
        String phoneNumber,
        String zipCode,
        String address,
        String addressDetail,
        int investScore,
        MemberRole role
) {
    public static MemberProfileResult from(MemberCredential memberCredential) {
        return new MemberProfileResult(
                memberCredential.memberId(),
                memberCredential.memberName(),
                memberCredential.memberEmail(),
                memberCredential.phoneNumber(),
                memberCredential.zipCode(),
                memberCredential.address(),
                memberCredential.addressDetail(),
                memberCredential.investScore(),
                memberCredential.role()
        );
    }
}
