package coin.coinzzickmock.providers.telemetry;

import java.util.Map;
import java.util.function.Supplier;

public interface TelemetryProvider {
    void recordUseCase(String useCaseName);

    void recordFailure(String useCaseName, String reason);

    void recordEvent(String eventName, Map<String, String> tags);

    void registerGauge(String gaugeName, Map<String, String> tags, Supplier<Number> valueSupplier);
}
