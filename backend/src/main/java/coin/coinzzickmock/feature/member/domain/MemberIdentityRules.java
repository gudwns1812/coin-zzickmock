package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

import java.util.regex.Pattern;

public final class MemberIdentityRules {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NICKNAME_ALLOWED = Pattern.compile("^[\\p{L}\\p{N} _-]+$");

    private MemberIdentityRules() {
    }

    public static String normalizeMemberId(String memberId) {
        return normalizeAccount(memberId);
    }

    public static String normalizeAccount(String account) {
        return normalizeRequired(account, "아이디");
    }

    public static String normalizeNickname(String nickname) {
        String normalized = normalizeWhitespace(nickname);
        int codePointCount = normalized.codePointCount(0, normalized.length());
        if (codePointCount < 2 || codePointCount > 30) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "닉네임은 2자 이상 30자 이하여야 합니다.");
        }
        if (!NICKNAME_ALLOWED.matcher(normalized).matches()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "닉네임에는 한글, 영문, 숫자, 공백, _, -만 사용할 수 있습니다.");
        }
        if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "닉네임에는 문자 또는 숫자가 포함되어야 합니다.");
        }
        return normalized;
    }

    public static String requirePasswordInput(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "비밀번호는 필수입니다.");
        }
        return rawPassword;
    }

    public static String validateRawPassword(String rawPassword) {
        String requiredPassword = requirePasswordInput(rawPassword);
        if (requiredPassword.length() < 8 || requiredPassword.length() > 20) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상 20자 이하여야 합니다.");
        }
        return requiredPassword;
    }

    static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    static String validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, fieldName + "은(는) 필수입니다.");
        }
        return value;
    }

    static String normalizeAddressDetail(String addressDetail) {
        return addressDetail == null ? "" : addressDetail.trim();
    }

    private static String normalizeWhitespace(String value) {
        validateRequired(value, "닉네임");
        return WHITESPACE.matcher(value.trim()).replaceAll(" ");
    }
}
