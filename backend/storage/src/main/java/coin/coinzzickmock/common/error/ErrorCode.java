package coin.coinzzickmock.common.error;

import org.slf4j.event.Level;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(404, "계정을 찾을 수 없습니다.", Level.INFO),
    ACCOUNT_CHANGED(409, "계정 잔고가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MEMBER_NOT_FOUND(404, "회원 정보를 찾을 수 없습니다.", Level.INFO),
    POSITION_NOT_FOUND(404, "포지션을 찾을 수 없습니다.", Level.INFO),
    POSITION_CHANGED(409, "포지션이 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MARKET_NOT_FOUND(404, "마켓 정보를 찾을 수 없습니다.", Level.INFO),
    REWARD_REDEMPTION_NOT_FOUND(404, "교환권 요청을 찾을 수 없습니다.", Level.INFO),
    REWARD_REDEMPTION_CONFLICT(409, "교환권 요청 상태가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    INSUFFICIENT_AVAILABLE_MARGIN(400, "사용 가능 증거금이 부족합니다.", Level.DEBUG),
    MEMBER_ALREADY_EXISTS(409, "이미 사용 중인 아이디입니다.", Level.INFO),
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다.", Level.DEBUG),
    UNAUTHORIZED(401, "로그인이 필요합니다.", Level.DEBUG),
    FORBIDDEN(403, "요청한 작업을 수행할 권한이 없습니다.", Level.INFO),
    TOO_MANY_REQUESTS(429, "요청이 너무 많습니다.", Level.INFO),
    INVALID_REQUEST(400, "잘못된 요청입니다.", Level.DEBUG),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.", Level.ERROR);

    private final int httpStatusCode;
    private final String message;
    private final Level logLevel;

    ErrorCode(int httpStatusCode, String message, Level logLevel) {
        this.httpStatusCode = httpStatusCode;
        this.message = message;
        this.logLevel = logLevel;
    }

    public int httpStatusCode() {
        return httpStatusCode;
    }

    public String message() {
        return message;
    }

    public Level logLevel() {
        return logLevel;
    }
}
