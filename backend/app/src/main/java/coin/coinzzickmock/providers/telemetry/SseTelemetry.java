package coin.coinzzickmock.providers.telemetry;

import java.time.Duration;

public interface SseTelemetry {
    void connectionOpened(String stream);

    void connectionClosed(String stream, String reason);

    void connectionRejected(String stream, String reason);

    void sendRecorded(String stream, String result, Duration duration);

    void executorRejected(String stream);
}
