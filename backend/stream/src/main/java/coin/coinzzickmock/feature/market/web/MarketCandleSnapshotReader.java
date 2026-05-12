package coin.coinzzickmock.feature.market.web;

import java.util.Optional;

public interface MarketCandleSnapshotReader {
    Optional<MarketCandleResponse> latest(String symbol, String interval);
}
