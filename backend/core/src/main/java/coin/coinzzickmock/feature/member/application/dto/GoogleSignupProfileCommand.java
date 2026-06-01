package coin.coinzzickmock.feature.member.application.dto;

public record GoogleSignupProfileCommand(
        String memberName,
        String nickname,
        String memberEmail,
        String phoneNumber,
        boolean requiredAgreement
) {
}
