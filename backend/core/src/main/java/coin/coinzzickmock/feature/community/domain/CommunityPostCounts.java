package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityPostCounts(long viewCount, long likeCount, long commentCount) {
    public CommunityPostCounts {
        if (viewCount < 0 || likeCount < 0 || commentCount < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public static CommunityPostCounts zero() {
        return new CommunityPostCounts(0, 0, 0);
    }

    public CommunityPostCounts viewed() {
        return new CommunityPostCounts(viewCount + 1, likeCount, commentCount);
    }

    public CommunityPostCounts liked() {
        return new CommunityPostCounts(viewCount, likeCount + 1, commentCount);
    }

    public CommunityPostCounts unliked() {
        return new CommunityPostCounts(viewCount, Math.max(0, likeCount - 1), commentCount);
    }

    public CommunityPostCounts commented() {
        return new CommunityPostCounts(viewCount, likeCount, commentCount + 1);
    }

    public CommunityPostCounts uncommented() {
        return new CommunityPostCounts(viewCount, likeCount, Math.max(0, commentCount - 1));
    }
}
