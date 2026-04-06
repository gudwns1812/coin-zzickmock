package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum CommonErrorType implements ErrorType {

    INVALID_REQUEST("COMMON_400", "잘못된 요청입니다.", LogLevel.WARN, HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("COMMON_500", "서버 내부 오류가 발생했습니다.", LogLevel.ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

    private final String errorCode;
    private final String message;
    private final LogLevel logLevel;
    private final HttpStatus httpStatus;

    CommonErrorType(String errorCode, String message, LogLevel logLevel, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
