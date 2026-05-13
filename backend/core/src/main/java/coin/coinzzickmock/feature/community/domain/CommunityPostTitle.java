package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityPostTitle(String value) {
    public static final int MAX_LENGTH = 200;

    public CommunityPostTitle {
        if (value == null) {
            throw invalid();
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
