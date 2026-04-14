package coin.coinzzickmock.support.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import coin.coinzzickmock.extern.exception.ExternalException;
import coin.coinzzickmock.support.response.ApiResponse;

@RestControllerAdvice
@Slf4j
public class CoreExceptionHandler {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoreException(CoreException exception) {
        return error(exception.getErrorType(), exception);
    }

    @ExceptionHandler(ExternalException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalException(ExternalException exception) {
        return error(exception.getErrorType(), exception);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return error(AuthErrorType.ACCESS_DENIED, exception);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception) {
        return error(CommonErrorType.INVALID_REQUEST, exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return error(CommonErrorType.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorType errorType, Exception exception) {
        writeLog(errorType.getLogLevel(), exception);
        return ResponseEntity.status(errorType.getHttpStatus())
                .body(ApiResponse.error(null, errorType));
    }

    private void writeLog(LogLevel logLevel, Exception exception) {
        switch (logLevel) {
            case TRACE -> log.trace(exception.getMessage(), exception);
            case DEBUG -> log.debug(exception.getMessage(), exception);
            case INFO -> log.info(exception.getMessage(), exception);
            case WARN -> log.warn(exception.getMessage(), exception);
            case ERROR -> log.error(exception.getMessage(), exception);
        }
    }
}
