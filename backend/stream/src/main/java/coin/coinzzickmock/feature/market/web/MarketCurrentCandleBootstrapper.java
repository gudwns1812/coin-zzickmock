package coin.coinzzickmock.feature.market.web;

/**
 * Stream-side port for preparing the latest candle snapshot before opening an SSE stream.
 *
 * <p>Bootstrapping means the app layer may synchronously load or derive the currently open candle
 * for the requested market so the stream layer can send an immediate initial snapshot. The
 * operation must be idempotent for the same {@code symbol}/{@code interval} pair and may perform
 * blocking provider/cache work in the app adapter.</p>
 *
 * <p>{@code symbol} is the exchange market symbol used by the futures API (for example
 * {@code BTCUSDT}). {@code interval} is the public candle interval value accepted by the market API
 * (for example {@code 1m} or {@code 1h}). Implementations may throw runtime exceptions from the app
 * boundary; callers release any reserved SSE capacity when that happens.</p>
 */
public interface MarketCurrentCandleBootstrapper {
    /**
     * Ensures a current candle snapshot exists when one is needed.
     *
     * @return {@code true} when this call performed bootstrap work; {@code false} when a snapshot
     * already existed, a recent attempt is in flight/throttled, or the adapter deliberately skipped
     * non-fatal bootstrap work.
     */
    boolean bootstrapIfNeeded(String symbol, String interval);
}
