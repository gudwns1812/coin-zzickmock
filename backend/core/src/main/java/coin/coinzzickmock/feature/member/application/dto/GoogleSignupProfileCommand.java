package coin.coinzzickmock.feature.member.application.dto;

public record GoogleSignupProfileCommand(
        String memberName,
        String nickname,
        String memberEmail,
        String phoneNumber,
        String zipCode,
        String address,
        String addressDetail,
        boolean requiredAgreement
) {
}
