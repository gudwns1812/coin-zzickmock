package coin.coinzzickmock.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException exception) {
        return ResponseEntity.status(exception.errorCode().httpStatus())
                .body(new ErrorResponse(exception.errorCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException() {
        // SSE 연결이 이미 끊긴 뒤 발생하는 비동기 종료 예외는 조용히 흡수한다.
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnhandledException(Exception exception, HttpServletResponse response) {
        if (isEventStreamResponse(response)) {
            return ResponseEntity.noContent().build();
        }

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
}
