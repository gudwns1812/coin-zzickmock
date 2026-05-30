package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberOAuthPendingLinkTest {
    @Test
    void createNormalizesProviderPayload() {
        MemberOAuthPendingLink pending = MemberOAuthPendingLink.create(
                "token-hash",
                " google ",
                " subject-1 ",
                " user@example.com ",
                " Google User ",
                Instant.parse("2026-05-30T00:10:00Z")
        );

        assertEquals("google", pending.provider());
        assertEquals("subject-1", pending.providerSubject());
        assertEquals("user@example.com", pending.providerEmail());
        assertEquals("Google User", pending.providerName());
        assertEquals(0, pending.attemptCount());
        assertNull(pending.consumedAt());
    }

    @Test
    void validateConsumableRejectsConsumedExpiredAndTooManyAttempts() {
        Instant now = Instant.parse("2026-05-30T00:00:00Z");
        MemberOAuthPendingLink pending = MemberOAuthPendingLink.create(
                "token-hash",
                OAuthProvider.GOOGLE.value(),
                "subject-1",
                null,
                null,
                now.plusSeconds(60)
        );

        CoreException consumed = assertThrows(CoreException.class, () -> pending.consume(now).validateConsumable(now));
        assertEquals(ErrorCode.OAUTH_ONBOARDING_CONSUMED, consumed.errorCode());

        CoreException expired = assertThrows(CoreException.class, () -> MemberOAuthPendingLink.create(
                "expired-token",
                OAuthProvider.GOOGLE.value(),
                "subject-2",
                null,
                null,
                now
        ).validateConsumable(now));
        assertEquals(ErrorCode.OAUTH_ONBOARDING_EXPIRED, expired.errorCode());

        MemberOAuthPendingLink failed = pending;
        for (int index = 0; index < MemberOAuthPendingLink.MAX_FAILED_LINK_ATTEMPTS; index++) {
            failed = failed.recordFailedAttempt(now);
        }
        MemberOAuthPendingLink terminal = failed;
        CoreException tooManyAttempts = assertThrows(CoreException.class, () -> terminal.validateConsumable(now));
        assertEquals(ErrorCode.OAUTH_LINK_TOO_MANY_ATTEMPTS, tooManyAttempts.errorCode());
        assertEquals(MemberOAuthPendingLink.MAX_FAILED_LINK_ATTEMPTS, terminal.attemptCount());
        assertEquals(now, terminal.consumedAt());
    }
}
