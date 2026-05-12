package coin.coinzzickmock.feature.market.web;

import java.util.Optional;

/**
 * Stream-side read port for the latest candle snapshot.
 *
 * <p>Implementations supplied by {@code :app} must treat {@code symbol} and {@code interval} as
 * required, non-blank API values and return {@link Optional#empty()} when no snapshot is currently
 * available.</p>
 */
public interface MarketCandleSnapshotReader {
    Optional<MarketCandleResponse> latest(String symbol, String interval);
}
