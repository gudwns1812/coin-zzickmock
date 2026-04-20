package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberIdentityRulesTest {
    @Test
    void validatesRawPasswordLengthForRegistration() {
        assertThrows(CoreException.class, () -> MemberIdentityRules.validateRawPassword("short"));
        assertEquals("hello@1234", MemberIdentityRules.validateRawPassword("hello@1234"));
    }

    @Test
    void rejectsBlankPasswordInputForAuthentication() {
        assertThrows(CoreException.class, () -> MemberIdentityRules.requirePasswordInput("   "));
    }
}
