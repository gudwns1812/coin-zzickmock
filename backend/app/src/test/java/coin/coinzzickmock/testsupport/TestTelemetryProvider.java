package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.Map;
import java.util.function.Supplier;

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

    @Override
    public void registerGauge(String gaugeName, Map<String, String> tags, Supplier<Number> valueSupplier) {
    }
}
