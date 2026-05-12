package coin.coinzzickmock.providers.telemetry;

import java.time.Duration;

public final class NoopSseTelemetry implements SseTelemetry {
    public static final NoopSseTelemetry INSTANCE = new NoopSseTelemetry();

    private NoopSseTelemetry() {
    }

    @Override
    public void connectionOpened(String stream) {
    }

    @Override
    public void connectionClosed(String stream, String reason) {
    }

    @Override
    public void connectionRejected(String stream, String reason) {
    }

    @Override
    public void sendRecorded(String stream, String result, Duration duration) {
    }

    @Override
    public void executorRejected(String stream) {
    }
}
