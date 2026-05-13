package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityAuthor(Long memberId, String nickname) {
    private static final int MAX_NICKNAME_LENGTH = 100;

    public CommunityAuthor {
        if (memberId == null || memberId <= 0) {
            throw invalid();
        }
        if (nickname == null || nickname.isBlank() || nickname.length() > MAX_NICKNAME_LENGTH) {
            throw invalid();
        }
        nickname = nickname.trim();
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
