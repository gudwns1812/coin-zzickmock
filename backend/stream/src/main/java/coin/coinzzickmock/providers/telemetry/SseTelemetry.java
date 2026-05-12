package coin.coinzzickmock.providers.telemetry;

import java.time.Duration;

/**
 * Public stream-module telemetry port for Server-Sent Events lifecycle and delivery metrics.
 *
 * <p>Implementations are provided by the app runtime; stream brokers call this port at connection,
 * rejection, send, and executor-boundary events. The {@code stream} parameter is the stable
 * telemetry stream identifier such as {@code market}, {@code market_candle}, or
 * {@code trading_execution}.</p>
 */
public interface SseTelemetry {
    /**
     * Records that a stream connection was opened and registered.
     *
     * @param stream stable stream identifier
     */
    void connectionOpened(String stream);

    /**
     * Records that a registered stream connection was closed.
     *
     * @param stream stable stream identifier
     * @param reason human-readable close reason such as timeout, error, or client_complete
     */
    void connectionClosed(String stream, String reason);

    /**
     * Records that a connection attempt was rejected before registration.
     *
     * @param stream stable stream identifier
     * @param reason rejection reason such as total_limit, symbol_limit, or key_limit
     */
    void connectionRejected(String stream, String reason);

    /**
     * Records a completed emitter send attempt.
     *
     * @param stream stable stream identifier
     * @param result send result label, typically success or failure
     * @param duration elapsed time spent in the send boundary
     */
    void sendRecorded(String stream, String result, Duration duration);

    /**
     * Records that the asynchronous delivery executor rejected a stream fan-out task.
     *
     * @param stream stable stream identifier
     */
    void executorRejected(String stream);
}
