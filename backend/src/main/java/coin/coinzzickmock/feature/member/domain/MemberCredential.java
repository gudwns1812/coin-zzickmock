package coin.coinzzickmock.feature.member.domain;

public record MemberCredential(
        String memberId,
        String passwordHash,
        String memberName,
        String memberEmail,
        String phoneNumber,
        String zipCode,
        String address,
        String addressDetail,
        int investScore
) {
    public static MemberCredential register(
            String memberId,
            String passwordHash,
            String memberName,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail,
            int investScore
    ) {
        return new MemberCredential(
                MemberIdentityRules.normalizeMemberId(memberId),
                MemberIdentityRules.validateRequired(passwordHash, "비밀번호 해시"),
                MemberIdentityRules.normalizeRequired(memberName, "이름"),
                MemberIdentityRules.normalizeRequired(memberEmail, "이메일"),
                MemberIdentityRules.normalizeRequired(phoneNumber, "휴대폰 번호"),
                MemberIdentityRules.normalizeRequired(zipCode, "우편번호"),
                MemberIdentityRules.normalizeRequired(address, "주소"),
                MemberIdentityRules.normalizeAddressDetail(addressDetail),
                investScore
        );
    }
}
