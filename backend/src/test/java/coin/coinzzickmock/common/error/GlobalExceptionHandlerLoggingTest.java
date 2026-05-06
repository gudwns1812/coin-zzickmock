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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.HandlerMapping;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerLoggingTest {
    @Test
    void mapsCoreExceptionToErrorCodeResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleCoreException(
                new CoreException(ErrorCode.MARKET_NOT_FOUND),
                request("GET", "/api/futures/markets/BTCUSDT", "/api/futures/markets/{symbol}")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.MARKET_NOT_FOUND.name(), ErrorCode.MARKET_NOT_FOUND.message()));
    }

    @Test
    void doesNotUseExceptionMessageAsPublicResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        CoreException exception = new CoreException(ErrorCode.INTERNAL_SERVER_ERROR) {
            @Override
            public String getMessage() {
                return "db password leaked";
            }
        };

        ResponseEntity<ErrorResponse> response = handler.handleCoreException(
                exception,
                request("POST", "/api/internal/refill?memberId=1", "/api/internal/refill")
        );

        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.message()
                ));
        assertThat(response.getBody().message()).doesNotContain("db password");
    }

    @Test
    void mapsMalformedRequestBodyToInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new HttpMessageNotReadableException(
                        "Malformed JSON request",
                        Mockito.mock(HttpInputMessage.class)
                ),
                request("POST", "/api/futures/orders", "/api/futures/orders")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.message()));
        assertThat(response.getBody().message()).doesNotContain("Malformed JSON request");
    }

    @Test
    void mapsMissingRequestParameterToInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new MissingServletRequestParameterException("interval", "String"),
                request("GET", "/api/futures/candles", "/api/futures/candles")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.message()));
    }

    @Test
    void logsHandledCoreExceptionAtErrorLevel(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        handler.handleCoreException(
                new CoreException(ErrorCode.INTERNAL_SERVER_ERROR),
                request("POST", "/api/internal/refill?memberId=1", "/api/internal/refill")
        );

        assertThat(output)
                .contains("Handled core exception")
                .contains("errorCode=INTERNAL_SERVER_ERROR")
                .contains("status=500")
                .contains("method=POST")
                .contains("pathPattern=/api/internal/refill")
                .doesNotContain("memberId=1");
    }

    @Test
    void logsHandledCoreExceptionAtInfoLevel(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        handler.handleCoreException(
                new CoreException(ErrorCode.ACCOUNT_CHANGED),
                request("POST", "/api/futures/orders", "/api/futures/orders")
        );

        assertThat(output)
                .contains("Handled core exception")
                .contains("errorCode=ACCOUNT_CHANGED")
                .contains("status=409")
                .contains("pathPattern=/api/futures/orders");
    }

    @Test
    void logsUnhandledNonSseException(CapturedOutput output) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                .thenReturn("/api/futures/markets");
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
                .contains("pathPattern=/api/futures/markets")
                .contains("unexpected failure");
    }

    private MockHttpServletRequest request(String method, String uri, String pathPattern) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pathPattern);
        return request;
    }
}
