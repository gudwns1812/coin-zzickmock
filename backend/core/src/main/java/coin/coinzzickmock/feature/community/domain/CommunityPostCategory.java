package coin.coinzzickmock.feature.community.domain;

public enum CommunityPostCategory {
    NOTICE,
    CHART_ANALYSIS,
    COIN_INFORMATION,
    CHAT;

    public boolean notice() {
        return this == NOTICE;
    }

    public boolean normalPost() {
        return this != NOTICE;
    }
}
