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
}
