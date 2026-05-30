package coin.coinzzickmock.feature.member.application.dto;

import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;

public record GoogleOnboardingStatusResult(
        String emailHint,
        String nameHint
) {
    public static GoogleOnboardingStatusResult from(MemberOAuthPendingLink pendingLink) {
        return new GoogleOnboardingStatusResult(pendingLink.providerEmail(), pendingLink.providerName());
    }
}
