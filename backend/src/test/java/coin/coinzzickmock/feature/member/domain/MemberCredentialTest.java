package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                " 04524 ",
                " 서울 중구 세종대로 110 ",
                " 12층 ",
                0
        );

        assertEquals("demo-user", credential.account());
        assertEquals("Demo Nick", credential.nickname());
        assertEquals("Demo User", credential.memberName());
        assertEquals("demo@coinzzickmock.dev", credential.memberEmail());
        assertEquals("010-1111-2222", credential.phoneNumber());
        assertEquals("04524", credential.zipCode());
        assertEquals("서울 중구 세종대로 110", credential.address());
        assertEquals("12층", credential.addressDetail());
    }

    @Test
    void rejectsBlankRequiredMemberId() {
        assertThrows(CoreException.class, () -> MemberIdentityRules.normalizeMemberId("   "));
    }
}
