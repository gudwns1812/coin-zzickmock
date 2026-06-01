package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record MemberOAuthPendingLink(
        Long id,
        String tokenHash,
        String provider,
        String providerSubject,
        String providerEmail,
        String providerName,
        Instant expiresAt,
        Instant consumedAt,
        int attemptCount,
        Instant lastFailedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static final int MAX_FAILED_LINK_ATTEMPTS = 5;

    public static MemberOAuthPendingLink create(
            String tokenHash,
            String provider,
            String providerSubject,
            String providerEmail,
            String providerName,
            Instant expiresAt
    ) {
        if (tokenHash == null || tokenHash.isBlank() || expiresAt == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return new MemberOAuthPendingLink(
                null,
                tokenHash,
                OAuthIdentityRules.normalizeProvider(provider),
                OAuthIdentityRules.normalizeProviderSubject(providerSubject),
                OAuthIdentityRules.normalizeOptional(providerEmail),
                OAuthIdentityRules.normalizeOptional(providerName),
                expiresAt,
                null,
                0,
                null,
                null,
                null
        );
    }

    public void validateConsumable(Instant now) {
        if (attemptCount >= MAX_FAILED_LINK_ATTEMPTS) {
            throw new CoreException(ErrorCode.OAUTH_LINK_TOO_MANY_ATTEMPTS);
        }
        if (consumedAt != null) {
            throw new CoreException(ErrorCode.OAUTH_ONBOARDING_CONSUMED);
        }
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new CoreException(ErrorCode.OAUTH_ONBOARDING_EXPIRED);
        }
    }

    public MemberOAuthPendingLink recordFailedAttempt(Instant now) {
        int nextAttemptCount = attemptCount + 1;
        Instant terminalConsumedAt = nextAttemptCount >= MAX_FAILED_LINK_ATTEMPTS ? now : consumedAt;
        return new MemberOAuthPendingLink(
                id,
                tokenHash,
                provider,
                providerSubject,
                providerEmail,
                providerName,
                expiresAt,
                terminalConsumedAt,
                nextAttemptCount,
                now,
                createdAt,
                updatedAt
        );
    }

    public MemberOAuthPendingLink consume(Instant now) {
        return new MemberOAuthPendingLink(
                id,
                tokenHash,
                provider,
                providerSubject,
                providerEmail,
                providerName,
                expiresAt,
                now,
                attemptCount,
                lastFailedAt,
                createdAt,
                updatedAt
        );
    }
}
