package coin.coinzzickmock.feature.community.domain;

public enum CommunityCategory {
    NOTICE,
    CHART_ANALYSIS,
    COIN_INFORMATION,
    CHAT;

    public boolean isNotice() {
        return this == NOTICE;
    }
}
