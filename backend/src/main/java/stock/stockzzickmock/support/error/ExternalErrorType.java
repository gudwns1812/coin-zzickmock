package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ExternalErrorType implements ErrorType {

    KRX_OTP_GENERATION_FAILED("EXTERNAL_500_KRX_OTP_GENERATION_FAILED", "KRX OTP 발급에 실패했습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR),
    KRX_HOLIDAY_REQUEST_FAILED("EXTERNAL_500_KRX_HOLIDAY_REQUEST_FAILED", "KRX 휴장일 조회에 실패했습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR),
    KRX_HOLIDAY_CALENDAR_NOT_READY("EXTERNAL_500_KRX_HOLIDAY_CALENDAR_NOT_READY", "KRX 휴장일 캘린더가 준비되지 않았습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR),
    KRX_HOLIDAY_RESPONSE_PARSE_FAILED("EXTERNAL_500_KRX_HOLIDAY_RESPONSE_PARSE_FAILED", "KRX 휴장일 응답 파싱에 실패했습니다.", LogLevel.ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR);

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
