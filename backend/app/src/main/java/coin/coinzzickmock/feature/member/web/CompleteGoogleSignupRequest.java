package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.dto.GoogleSignupProfileCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record CompleteGoogleSignupRequest(
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String email,
        @NotBlank String phoneNumber,
        @AssertTrue boolean agreement
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
