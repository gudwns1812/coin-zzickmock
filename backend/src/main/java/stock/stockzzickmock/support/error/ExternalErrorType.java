package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ExternalErrorType implements ErrorType {

    HOLIDAY_API_PARSE_FAILED("EXTERNAL_500_HOLIDAY_API_PARSE_FAILED", "공휴일 API 파싱 실패", LogLevel.ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

    private final String errorCode;
    private final String message;
    private final LogLevel logLevel;
    private final HttpStatus httpStatus;

    ExternalErrorType(String errorCode, String message, LogLevel logLevel, HttpStatus httpStatus) {
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
