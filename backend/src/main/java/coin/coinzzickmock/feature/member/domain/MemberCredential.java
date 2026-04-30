package coin.coinzzickmock.feature.member.domain;

public record MemberCredential(
        Long memberId,
        String account,
        String passwordHash,
        String memberName,
        String nickname,
        String memberEmail,
        String phoneNumber,
        String zipCode,
        String address,
        String addressDetail,
        int investScore,
        MemberRole role
) {
    public static MemberCredential register(
            String account,
            String passwordHash,
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail,
            int investScore
    ) {
        return new MemberCredential(
                null,
                MemberIdentityRules.normalizeAccount(account),
                MemberIdentityRules.validateRequired(passwordHash, "비밀번호 해시"),
                MemberIdentityRules.normalizeRequired(memberName, "이름"),
                MemberIdentityRules.normalizeNickname(nickname),
                MemberIdentityRules.normalizeRequired(memberEmail, "이메일"),
                MemberIdentityRules.normalizeRequired(phoneNumber, "휴대폰 번호"),
                MemberIdentityRules.normalizeRequired(zipCode, "우편번호"),
                MemberIdentityRules.normalizeRequired(address, "주소"),
                MemberIdentityRules.normalizeAddressDetail(addressDetail),
                investScore,
                MemberRole.USER
        );
    }

    public MemberCredential withMemberId(Long memberId) {
        return new MemberCredential(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                investScore,
                role
        );
    }

    public MemberCredential asAdmin() {
        return withRole(MemberRole.ADMIN);
    }

    public MemberCredential withRole(MemberRole role) {
        return new MemberCredential(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                investScore,
                role == null ? MemberRole.USER : role
        );
    }
}
