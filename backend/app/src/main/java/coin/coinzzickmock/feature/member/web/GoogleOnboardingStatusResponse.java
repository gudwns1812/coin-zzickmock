package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.dto.GoogleOnboardingStatusResult;

public record GoogleOnboardingStatusResponse(
        boolean active,
        String emailHint,
        String nameHint
) {
    static GoogleOnboardingStatusResponse from(GoogleOnboardingStatusResult result) {
        return new GoogleOnboardingStatusResponse(true, result.emailHint(), result.nameHint());
    }
}
