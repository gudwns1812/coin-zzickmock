package coin.coinzzickmock.feature.member.domain;

import java.time.Instant;

public record MemberOAuthIdentity(
        Long id,
        Long memberId,
        String provider,
        String providerSubject,
        String providerEmail,
        String providerName,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemberOAuthIdentity google(
            Long memberId,
            String providerSubject,
            String providerEmail,
            String providerName
    ) {
        return new MemberOAuthIdentity(
                null,
                memberId,
                OAuthProvider.GOOGLE.value(),
                OAuthIdentityRules.normalizeProviderSubject(providerSubject),
                OAuthIdentityRules.normalizeOptional(providerEmail),
                OAuthIdentityRules.normalizeOptional(providerName),
                null,
                null
        );
    }
}
