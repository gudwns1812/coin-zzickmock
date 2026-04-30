package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.telemetry.TelemetryProvider;

public class NoopTelemetryProvider implements TelemetryProvider {
    @Override
    public void recordUseCase(String useCaseName) {
    }

    @Override
    public void recordFailure(String useCaseName, String reason) {
    }
}
