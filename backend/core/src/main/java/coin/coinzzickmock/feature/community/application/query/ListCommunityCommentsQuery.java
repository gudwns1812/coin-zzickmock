package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record ListCommunityCommentsQuery(Long postId, int page, int size) {
    private static final int MAX_SIZE = 100;

    public ListCommunityCommentsQuery {
        if (postId == null || postId <= 0 || page < 0 || size < 1 || size > MAX_SIZE) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
