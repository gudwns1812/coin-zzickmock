package coin.coinzzickmock.feature.market.web;

public record MarketStreamSessionKey(Long memberId, String clientKey) {
    public MarketStreamSessionKey {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        if (clientKey == null || clientKey.isBlank()) {
            throw new IllegalArgumentException("clientKey is required");
        }
    }
}
