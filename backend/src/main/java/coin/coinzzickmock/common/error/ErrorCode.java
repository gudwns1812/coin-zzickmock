package coin.coinzzickmock.common.error;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.", Level.INFO),
    ACCOUNT_CHANGED(HttpStatus.CONFLICT, "계정 잔고가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다.", Level.INFO),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "포지션을 찾을 수 없습니다.", Level.INFO),
    POSITION_CHANGED(HttpStatus.CONFLICT, "포지션이 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MARKET_NOT_FOUND(HttpStatus.NOT_FOUND, "마켓 정보를 찾을 수 없습니다.", Level.INFO),
    REWARD_REDEMPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "교환권 요청을 찾을 수 없습니다.", Level.INFO),
    REWARD_REDEMPTION_CONFLICT(HttpStatus.CONFLICT, "교환권 요청 상태가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    INSUFFICIENT_AVAILABLE_MARGIN(HttpStatus.BAD_REQUEST, "사용 가능 증거금이 부족합니다.", Level.DEBUG),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.", Level.INFO),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.", Level.DEBUG),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", Level.DEBUG),
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청한 작업을 수행할 권한이 없습니다.", Level.INFO),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다.", Level.INFO),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", Level.DEBUG),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", Level.ERROR);

    private final HttpStatus httpStatus;
    private final String message;
    private final Level logLevel;

    ErrorCode(HttpStatus httpStatus, String message, Level logLevel) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }

    public Level logLevel() {
        return logLevel;
    }
}
