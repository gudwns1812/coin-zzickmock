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
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (!NICKNAME_ALLOWED.matcher(normalized).matches()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    public static String requirePasswordInput(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return rawPassword;
    }

    public static String validateRawPassword(String rawPassword) {
        String requiredPassword = requirePasswordInput(rawPassword);
        if (requiredPassword.length() < 8 || requiredPassword.length() > 20) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return requiredPassword;
    }

    static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    static String validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
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
