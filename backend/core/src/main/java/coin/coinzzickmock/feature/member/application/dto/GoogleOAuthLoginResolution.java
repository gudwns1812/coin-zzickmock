package coin.coinzzickmock.feature.member.application.dto;

public record GoogleOAuthLoginResolution(
        boolean linked,
        MemberProfileResult memberProfile
) {
    public static GoogleOAuthLoginResolution linked(MemberProfileResult memberProfile) {
        return new GoogleOAuthLoginResolution(true, memberProfile);
    }

    public static GoogleOAuthLoginResolution needsOnboarding() {
        return new GoogleOAuthLoginResolution(false, null);
    }
}
