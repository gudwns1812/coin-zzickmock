package coin.coinzzickmock.providers.telemetry;

import java.util.Map;

public interface TelemetryProvider {
    void recordUseCase(String useCaseName);

    void recordFailure(String useCaseName, String reason);

    void recordEvent(String eventName, Map<String, String> tags);
}
