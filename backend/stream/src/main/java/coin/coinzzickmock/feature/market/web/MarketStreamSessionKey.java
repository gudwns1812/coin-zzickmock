package coin.coinzzickmock.feature.market.web;

public record MarketStreamSessionKey(Long memberId, String clientKey) {
    public MarketStreamSessionKey {
        if (clientKey == null || clientKey.isBlank()) {
            throw new IllegalArgumentException("clientKey is required");
        }
    }
}
