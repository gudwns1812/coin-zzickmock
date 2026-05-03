package coin.coinzzickmock.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException exception) {
        return ResponseEntity.status(exception.errorCode().httpStatus())
                .body(new ErrorResponse(exception.errorCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
        // SSE 연결이 이미 끊긴 뒤 발생하는 비동기 종료 예외는 조용히 흡수한다.
        log.debug("Async request became unusable after the client disconnected.", exception);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        log.debug("Invalid client request.", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnhandledException(
            Exception exception,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (isEventStreamResponse(response)) {
            log.debug("Unhandled exception occurred after an event stream was established. method={} uri={}",
                    requestMethod(request), requestUri(request), exception);
            return ResponseEntity.noContent().build();
        }

        log.error("Unhandled server exception. method={} uri={}", requestMethod(request), requestUri(request), exception);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.message()
                ));
    }

    private boolean isEventStreamResponse(HttpServletResponse response) {
        return response != null
                && response.getContentType() != null
                && response.getContentType().startsWith("text/event-stream");
    }

    private String requestMethod(HttpServletRequest request) {
        return request == null ? null : request.getMethod();
    }

    private String requestUri(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }
}
