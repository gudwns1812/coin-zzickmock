package stock.stockzzickmock.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum AuthErrorType implements ErrorType {

    DUPLICATE_ACCOUNT("AUTH_409_DUPLICATE_ACCOUNT", "이미 사용 중인 아이디입니다.", LogLevel.WARN, HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_401_INVALID_CREDENTIALS", "아이디 / 비밀번호가 일치하지 않습니다.", LogLevel.WARN, HttpStatus.UNAUTHORIZED),
    INVALID_JWT("AUTH_401_INVALID_JWT", "토큰 검증에 실패했습니다.", LogLevel.WARN, HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("AUTH_401_REFRESH_TOKEN_NOT_FOUND", "refresh token 이 존재하지 않습니다.", LogLevel.WARN, HttpStatus.UNAUTHORIZED),
    MEMBER_NOT_FOUND("AUTH_404_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.", LogLevel.WARN, HttpStatus.NOT_FOUND),
    MEMBER_ACCESS_DENIED("AUTH_403_MEMBER_ACCESS_DENIED", "본인 정보만 수정할 수 있습니다.", LogLevel.WARN, HttpStatus.FORBIDDEN),
    AUTHENTICATION_REQUIRED("AUTH_401_AUTHENTICATION_REQUIRED", "인증이 필요합니다.", LogLevel.WARN, HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_403_ACCESS_DENIED", "접근 권한이 없습니다.", LogLevel.WARN, HttpStatus.FORBIDDEN);

    private final String errorCode;
    private final String message;
    private final LogLevel logLevel;
    private final HttpStatus httpStatus;

    AuthErrorType(String errorCode, String message, LogLevel logLevel, HttpStatus httpStatus) {
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
