package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public final class OAuthIdentityRules {
    private OAuthIdentityRules() {
    }

    public static String normalizeProvider(String provider) {
        return OAuthProvider.from(provider).value();
    }

    public static String normalizeProviderSubject(String providerSubject) {
        if (providerSubject == null || providerSubject.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = providerSubject.trim();
        if (normalized.length() > 255) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    public static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
