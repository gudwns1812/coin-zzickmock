package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketTradeMovementTelemetry {
    static final String QUEUE_DROP_TOTAL = "market.trade.movement.queue.drop.total";
    static final String QUEUE_SIZE_CURRENT = "market.trade.movement.queue.size.current";
    static final String WORKER_FAILURE_TOTAL = "market.trade.movement.worker.failure.total";

    private final TelemetryProvider telemetryProvider;

    @Autowired
    public MarketTradeMovementTelemetry(Providers providers) {
        this(providers.telemetry());
    }

    MarketTradeMovementTelemetry(TelemetryProvider telemetryProvider) {
        this.telemetryProvider = telemetryProvider;
    }

    void registerQueueSizeGauge(Supplier<Number> valueSupplier) {
        telemetryProvider.registerGauge(QUEUE_SIZE_CURRENT, Map.of(), valueSupplier);
    }

    void recordQueueDrop() {
        telemetryProvider.recordEvent(QUEUE_DROP_TOTAL, Map.of("reason", "full"));
    }

    void recordWorkerFailure() {
        telemetryProvider.recordEvent(WORKER_FAILURE_TOTAL, Map.of("reason", "runtime_exception"));
    }
}
