package coin.coinzzickmock.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerLoggingTest {
    @Test
    void logsUnhandledNonSseException(CapturedOutput output) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getRequestURI()).thenReturn("/api/futures/markets");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<?> response = handler.handleUnhandledException(
                new IllegalStateException("unexpected failure"),
                request,
                null
        );

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(output)
                .contains("Unhandled server exception")
                .contains("method=GET")
                .contains("uri=/api/futures/markets")
                .contains("unexpected failure");
    }
}
