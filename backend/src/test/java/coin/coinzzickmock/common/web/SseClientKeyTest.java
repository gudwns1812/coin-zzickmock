package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.error.CoreException;
import org.junit.jupiter.api.Test;

class SseClientKeyTest {
    @Test
    void resolvesTrimmedClientKey() {
        SseClientKey clientKey = SseClientKey.resolve("  tab-1  ");

        assertThat(clientKey.value()).isEqualTo("tab-1");
    }

    @Test
    void resolvesMissingOrBlankClientKeyToUniqueFallbacks() {
        SseClientKey missing = SseClientKey.resolve(null);
        SseClientKey blank = SseClientKey.resolve("   ");

        assertThat(missing.value()).isNotBlank();
        assertThat(blank.value()).isNotBlank();
        assertThat(missing.value()).isNotEqualTo(blank.value());
    }

    @Test
    void rejectsTooLongClientKey() {
        String tooLong = "a".repeat(129);

        assertThatThrownBy(() -> SseClientKey.resolve(tooLong))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void canonicalConstructorRejectsBlankValue() {
        assertThatThrownBy(() -> new SseClientKey(" "))
                .isInstanceOf(CoreException.class);
    }
}
