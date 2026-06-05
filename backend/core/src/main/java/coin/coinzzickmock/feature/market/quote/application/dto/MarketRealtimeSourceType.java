package coin.coinzzickmock.feature.market.quote.application.dto;

public enum MarketRealtimeSourceType {
    TRADE,
    TICKER,
    CANDLE,
    REST_BOOTSTRAP,
    REST_RECOVERY;

    public boolean isWebSocketSource() {
        return this == TRADE || this == TICKER || this == CANDLE;
    }

    public boolean isRestFallbackSource() {
        return this == REST_BOOTSTRAP || this == REST_RECOVERY;
    }
}
