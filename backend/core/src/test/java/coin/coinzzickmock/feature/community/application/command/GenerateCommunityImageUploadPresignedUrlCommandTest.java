package coin.coinzzickmock.feature.community.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class GenerateCommunityImageUploadPresignedUrlCommandTest {
    @Test
    void validatesRequiredFields() {
        new GenerateCommunityImageUploadPresignedUrlCommand(1L, "chart.png", "image/png", 1024L);

        assertThatThrownErrorCode(() -> new GenerateCommunityImageUploadPresignedUrlCommand(null, "chart.png", "image/png", 1024L), ErrorCode.INVALID_REQUEST);
        assertThatThrownErrorCode(() -> new GenerateCommunityImageUploadPresignedUrlCommand(0L, "chart.png", "image/png", 1024L), ErrorCode.INVALID_REQUEST);
        assertThatThrownErrorCode(() -> new GenerateCommunityImageUploadPresignedUrlCommand(1L, "", "image/png", 1024L), ErrorCode.INVALID_REQUEST);
        assertThatThrownErrorCode(() -> new GenerateCommunityImageUploadPresignedUrlCommand(1L, "chart.png", "", 1024L), ErrorCode.INVALID_REQUEST);
        assertThatThrownErrorCode(() -> new GenerateCommunityImageUploadPresignedUrlCommand(1L, "chart.png", "image/png", 0L), ErrorCode.INVALID_REQUEST);
    }

    private static void assertThatThrownErrorCode(Runnable runnable, ErrorCode errorCode) {
        CoreException exception = assertThrows(CoreException.class, runnable::run);
        assertThat(exception.errorCode()).isEqualTo(errorCode);
    }
}
