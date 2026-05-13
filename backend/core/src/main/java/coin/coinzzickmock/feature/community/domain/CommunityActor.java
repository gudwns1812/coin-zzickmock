package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityActor(Long memberId, boolean admin) {
    public CommunityActor {
        if (memberId == null || memberId <= 0) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }

    public boolean sameMember(Long otherMemberId) {
        return memberId.equals(otherMemberId);
    }
}
