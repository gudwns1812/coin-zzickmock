package coin.coinzzickmock.common.error;

import org.slf4j.event.Level;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(404, "계정을 찾을 수 없습니다.", Level.INFO),
    ACCOUNT_CHANGED(409, "계정 잔고가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MEMBER_NOT_FOUND(404, "회원 정보를 찾을 수 없습니다.", Level.INFO),
    POSITION_NOT_FOUND(404, "포지션을 찾을 수 없습니다.", Level.INFO),
    POSITION_CHANGED(409, "포지션이 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    MARKET_NOT_FOUND(404, "마켓 정보를 찾을 수 없습니다.", Level.INFO),
    MARKET_PRICE_STALE(409, "실시간 시세가 최신 상태가 아닙니다. 잠시 후 다시 시도해주세요.", Level.INFO),
    REWARD_REDEMPTION_NOT_FOUND(404, "교환권 요청을 찾을 수 없습니다.", Level.INFO),
    REWARD_REDEMPTION_CONFLICT(409, "교환권 요청 상태가 변경되었습니다. 다시 조회 후 시도해주세요.", Level.INFO),
    INSUFFICIENT_AVAILABLE_MARGIN(400, "사용 가능 증거금이 부족합니다.", Level.DEBUG),
    MEMBER_ALREADY_EXISTS(409, "이미 사용 중인 아이디입니다.", Level.INFO),
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다.", Level.DEBUG),
    PASSWORD_LOGIN_DISABLED(410, "비밀번호 로그인은 더 이상 지원하지 않습니다. Google 로그인으로 진행해주세요.", Level.INFO),
    OAUTH_LOGIN_DISABLED(503, "Google 로그인이 아직 설정되지 않았습니다.", Level.INFO),
    OAUTH_STATE_INVALID(401, "Google 로그인 요청이 만료되었습니다. 다시 시도해주세요.", Level.DEBUG),
    OAUTH_ONBOARDING_EXPIRED(401, "Google 가입 또는 연결 요청이 만료되었습니다. 다시 시도해주세요.", Level.DEBUG),
    OAUTH_ONBOARDING_CONSUMED(409, "이미 처리된 Google 가입 또는 연결 요청입니다.", Level.DEBUG),
    OAUTH_IDENTITY_ALREADY_LINKED(409, "이미 연결된 Google 계정입니다.", Level.INFO),
    OAUTH_LINK_TOO_MANY_ATTEMPTS(429, "계정 연결 시도 횟수를 초과했습니다. 다시 Google 로그인부터 시작해주세요.", Level.INFO),
    MISSING_REQUIRED_AGREEMENT(400, "필수 동의가 필요합니다.", Level.DEBUG),
    UNAUTHORIZED(401, "로그인이 필요합니다.", Level.DEBUG),
    FORBIDDEN(403, "요청한 작업을 수행할 권한이 없습니다.", Level.INFO),
    COMMUNITY_POST_INVALID_CATEGORY(400, "게시글 카테고리가 올바르지 않습니다.", Level.DEBUG),
    COMMUNITY_POST_INVALID_TITLE(400, "게시글 제목이 올바르지 않습니다.", Level.DEBUG),
    COMMUNITY_POST_INVALID_CONTENT(400, "게시글 내용이 올바르지 않습니다.", Level.DEBUG),
    COMMUNITY_POST_IMAGE_NOT_ATTACHABLE(400, "게시글 이미지를 첨부할 수 없습니다. 다시 업로드 후 시도해주세요.", Level.DEBUG),
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
