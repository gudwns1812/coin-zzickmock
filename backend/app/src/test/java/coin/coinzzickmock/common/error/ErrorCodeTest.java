package coin.coinzzickmock.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class ErrorCodeTest {
    @Test
    void exposesDefaultSlf4jLogLevelForEveryErrorCode() {
        assertThat(ErrorCode.values())
                .allSatisfy(errorCode -> assertThat(errorCode.logLevel()).isInstanceOf(Level.class));
    }

    @Test
    void usesRepresentativeDefaultLogLevelPolicy() {
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.logLevel()).isEqualTo(Level.ERROR);
        assertThat(ErrorCode.UNAUTHORIZED.logLevel()).isEqualTo(Level.DEBUG);
        assertThat(ErrorCode.INVALID_REQUEST.logLevel()).isEqualTo(Level.DEBUG);
        assertThat(ErrorCode.ACCOUNT_CHANGED.logLevel()).isEqualTo(Level.INFO);
        assertThat(ErrorCode.TOO_MANY_REQUESTS.logLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void keepsHandledClientAndBusinessFailuresBelowWarn() {
        assertThat(ErrorCode.values())
                .filteredOn(errorCode -> errorCode.httpStatusCode() >= 400 && errorCode.httpStatusCode() < 500)
                .allSatisfy(errorCode -> assertThat(errorCode.logLevel())
                        .as("%s should not create incident-level logs", errorCode)
                        .isNotIn(EnumSet.of(Level.WARN, Level.ERROR)));
    }
}
