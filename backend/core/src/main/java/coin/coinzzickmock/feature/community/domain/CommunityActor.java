package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityActor(Long memberId, boolean isAdmin) {
    public CommunityActor {
        if (memberId == null || memberId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public boolean isSameMember(Long otherMemberId) {
        return otherMemberId != null && memberId.equals(otherMemberId);
    }
}
