package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.function.Supplier;

public class MicrometerTelemetryProvider implements TelemetryProvider {
    private static final String USE_CASE_TOTAL = "app.usecase.total";
    private static final String USE_CASE_FAILURE_TOTAL = "app.usecase.failure.total";

    private final MeterRegistry meterRegistry;

    public MicrometerTelemetryProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordUseCase(String useCaseName) {
        meterRegistry.counter(USE_CASE_TOTAL, MetricTags.of(Map.of(
                "use_case", MetricTags.tagValue("use_case", useCaseName)
        ))).increment();
    }

    @Override
    public void recordFailure(String useCaseName, String reason) {
        meterRegistry.counter(USE_CASE_FAILURE_TOTAL, MetricTags.of(Map.of(
                "use_case", MetricTags.tagValue("use_case", useCaseName),
                "reason", MetricTags.tagValue("reason", reason)
        ))).increment();
    }

    @Override
    public void recordEvent(String eventName, Map<String, String> tags) {
        meterRegistry.counter(MetricTags.meterName(eventName), MetricTags.of(tags)).increment();
    }

    @Override
    public void registerGauge(String gaugeName, Map<String, String> tags, Supplier<Number> valueSupplier) {
        Gauge.builder(MetricTags.meterName(gaugeName), valueSupplier, supplier -> supplier.get().doubleValue())
                .tags(MetricTags.of(tags))
                .strongReference(true)
                .register(meterRegistry);
    }
}
