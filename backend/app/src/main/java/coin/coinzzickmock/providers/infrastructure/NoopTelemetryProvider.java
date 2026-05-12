package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.Map;

public class NoopTelemetryProvider implements TelemetryProvider {
    @Override
    public void recordUseCase(String useCaseName) {
    }

    @Override
    public void recordFailure(String useCaseName, String reason) {
    }

    @Override
    public void recordEvent(String eventName, Map<String, String> tags) {
    }
}
