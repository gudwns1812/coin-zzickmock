package coin.coinzzickmock.feature.market.application.realtime;

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
