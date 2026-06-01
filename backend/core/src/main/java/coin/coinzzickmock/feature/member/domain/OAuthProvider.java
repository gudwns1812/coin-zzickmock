package coin.coinzzickmock.feature.member.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public enum OAuthProvider {
    GOOGLE("google");

    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OAuthProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        for (OAuthProvider provider : values()) {
            if (provider.value.equals(value.trim().toLowerCase())) {
                return provider;
            }
        }
        throw new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
