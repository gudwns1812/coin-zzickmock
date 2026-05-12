package coin.coinzzickmock.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class CoreExceptionTest {
    @Test
    void hasOnlyErrorCodeConstructor() {
        assertThat(CoreException.class.getConstructors())
                .singleElement()
                .extracting(Constructor::getParameterTypes)
                .isEqualTo(new Class<?>[]{ErrorCode.class});
    }

    @Test
    void usesErrorCodeMessageAsRuntimeMessage() {
        CoreException exception = new CoreException(ErrorCode.INVALID_REQUEST);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_REQUEST.message());
    }
}
