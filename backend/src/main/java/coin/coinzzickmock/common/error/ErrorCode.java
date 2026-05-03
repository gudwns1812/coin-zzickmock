package coin.coinzzickmock.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."),
    ACCOUNT_CHANGED(HttpStatus.CONFLICT, "계정 잔고가 변경되었습니다. 다시 조회 후 시도해주세요."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "포지션을 찾을 수 없습니다."),
    POSITION_CHANGED(HttpStatus.CONFLICT, "포지션이 변경되었습니다. 다시 조회 후 시도해주세요."),
    MARKET_NOT_FOUND(HttpStatus.NOT_FOUND, "마켓 정보를 찾을 수 없습니다."),
    REWARD_REDEMPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "교환권 요청을 찾을 수 없습니다."),
    REWARD_REDEMPTION_CONFLICT(HttpStatus.CONFLICT, "교환권 요청 상태가 변경되었습니다. 다시 조회 후 시도해주세요."),
    INSUFFICIENT_AVAILABLE_MARGIN(HttpStatus.BAD_REQUEST, "사용 가능 증거금이 부족합니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청한 작업을 수행할 권한이 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
