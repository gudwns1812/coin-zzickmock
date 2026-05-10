package coin.coinzzickmock.feature.market.web;

import java.util.Objects;

public record MarketStreamSessionKey(Long memberId, String clientKey) {
    public MarketStreamSessionKey {
        Objects.requireNonNull(memberId, "memberId must not be null");
        if (clientKey == null || clientKey.isBlank()) {
            throw new IllegalArgumentException("clientKey must not be blank");
        }
    }
}
