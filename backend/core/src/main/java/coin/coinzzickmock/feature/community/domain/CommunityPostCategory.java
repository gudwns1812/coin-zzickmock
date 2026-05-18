package coin.coinzzickmock.feature.community.domain;

public enum CommunityPostCategory {
    NOTICE,
    CHART_ANALYSIS,
    COIN_INFORMATION,
    CHAT;

    public boolean isNotice() {
        return this == NOTICE;
    }

    public boolean isNormalPost() {
        return this != NOTICE;
    }
}
