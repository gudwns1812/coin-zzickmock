package coin.coinzzickmock.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerLoggingTest {
    @Test
    void mapsCoreExceptionToErrorResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleCoreException(
                new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.MARKET_NOT_FOUND.name(), "지원하지 않는 심볼입니다."));
    }

    @Test
    void mapsMalformedRequestBodyToInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new HttpMessageNotReadableException(
                        "Malformed JSON request",
                        Mockito.mock(HttpInputMessage.class)
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.message()));
    }

    @Test
    void mapsMissingRequestParameterToInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new MissingServletRequestParameterException("interval", "String")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.message()));
    }

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
