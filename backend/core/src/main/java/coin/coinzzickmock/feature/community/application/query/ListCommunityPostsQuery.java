package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;

public record ListCommunityPostsQuery(CommunityCategory category, int page, int size) {
    private static final int MAX_SIZE = 100;

    public ListCommunityPostsQuery {
        if (page < 0 || size < 1 || size > MAX_SIZE || (category != null && category.isNotice())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
