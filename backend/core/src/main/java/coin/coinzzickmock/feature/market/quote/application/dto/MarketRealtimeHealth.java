package coin.coinzzickmock.feature.market.quote.application.dto;

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
