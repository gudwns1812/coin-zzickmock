package coin.coinzzickmock.feature.market.application.dto;

public enum MarketRealtimeHealth {
    HEALTHY,
    BOOTSTRAPPING,
    RECOVERING,
    STALE,
    UNAVAILABLE;

    public boolean canSatisfyFreshness() {
        return this == HEALTHY || this == BOOTSTRAPPING || this == RECOVERING;
    }
}
