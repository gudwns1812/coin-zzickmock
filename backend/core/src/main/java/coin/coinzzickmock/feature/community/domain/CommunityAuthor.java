package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityAuthor(Long memberId, String nickname) {
    private static final int MAX_NICKNAME_LENGTH = 100;

    public CommunityAuthor {
        if (memberId == null || memberId <= 0) {
            throw invalid();
        }
        if (nickname == null) {
            throw invalid();
        }
        nickname = nickname.trim();
        if (nickname.isEmpty() || nickname.length() > MAX_NICKNAME_LENGTH) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
