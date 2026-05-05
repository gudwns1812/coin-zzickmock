package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.Map;

public abstract class TestTelemetryProvider implements TelemetryProvider {
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
