package coin.coinzzickmock.providers.telemetry;

import java.time.Duration;

public interface SseTelemetry {
    void connectionOpened(String stream);

    void connectionClosed(String stream, String reason);

    void connectionRejected(String stream, String reason);

    void sendRecorded(String stream, String result, Duration duration);

    void executorRejected(String stream);

    static SseTelemetry noop() {
        return new SseTelemetry() {
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
        };
    }
}
