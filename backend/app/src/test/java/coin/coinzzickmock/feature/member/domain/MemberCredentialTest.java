package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberCredentialTest {
    @Test
    void registerNormalizesAllRequiredFields() {
        MemberCredential credential = MemberCredential.register(
                " demo-user ",
                "hashed-password",
                " Demo User ",
                " Demo Nick ",
                " demo@coinzzickmock.dev ",
                " 010-1111-2222 ",
                0
        );

        assertEquals("demo-user", credential.account());
        assertEquals("Demo Nick", credential.nickname());
        assertEquals("Demo User", credential.memberName());
        assertEquals("demo@coinzzickmock.dev", credential.memberEmail());
        assertEquals("010-1111-2222", credential.phoneNumber());
    }

    @Test
    void registerGoogleOnlyKeepsLegacyCredentialFieldsEmpty() {
        MemberCredential credential = MemberCredential.registerGoogleOnly(
                " Google User ",
                " Google Nick ",
                " google-user@coinzzickmock.dev ",
                " 010-3333-4444 ",
                0
        );

        assertNull(credential.account());
        assertNull(credential.passwordHash());
        assertEquals("Google User", credential.memberName());
        assertEquals("Google Nick", credential.nickname());
        assertEquals("google-user@coinzzickmock.dev", credential.memberEmail());
    }

    @Test
    void rejectsBlankRequiredMemberId() {
        assertThrows(CoreException.class, () -> MemberIdentityRules.normalizeMemberId("   "));
    }
}
