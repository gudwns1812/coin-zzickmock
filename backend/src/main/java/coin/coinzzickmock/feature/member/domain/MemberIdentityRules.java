package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public final class MemberIdentityRules {
    private MemberIdentityRules() {
    }

    public static String normalizeMemberId(String memberId) {
        return normalizeRequired(memberId, "아이디");
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
}
