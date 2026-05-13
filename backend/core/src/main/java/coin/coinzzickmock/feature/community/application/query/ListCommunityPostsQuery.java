package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;

public record ListCommunityPostsQuery(CommunityCategory category, int page, int size) {
}
