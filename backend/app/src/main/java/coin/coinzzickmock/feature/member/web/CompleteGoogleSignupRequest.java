package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.dto.GoogleSignupProfileCommand;

public record CompleteGoogleSignupRequest(
        String name,
        String nickname,
        String email,
        String phoneNumber,
        boolean agreement
) {
    GoogleSignupProfileCommand toCommand() {
        return new GoogleSignupProfileCommand(
                name,
                nickname,
                email,
                phoneNumber,
                agreement
        );
    }
}
