package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SseClientKeyTest {
    @Test
    void resolvesTrimmedClientKey() {
        SseClientKey clientKey = SseClientKey.resolve("  tab-1  ");

        assertThat(clientKey.value()).isEqualTo("tab-1");
    }

    @Test
    void resolvesMissingOrBlankClientKeyToFallbacks() {
        SseClientKey missing = SseClientKey.resolve(null);
        SseClientKey blank = SseClientKey.resolve("   ");

        assertThat(missing.value()).isNotBlank();
        assertThat(blank.value()).isNotBlank();
    }

    @Test
    void rejectsTooLongClientKey() {
        String tooLong = "a".repeat(129);

        assertThatThrownBy(() -> SseClientKey.resolve(tooLong))
                .isInstanceOf(SseClientKeyException.class);
    }

    @Test
    void canonicalConstructorRejectsBlankValue() {
        assertThatThrownBy(() -> new SseClientKey(" "))
                .isInstanceOf(SseClientKeyException.class);
    }
}
