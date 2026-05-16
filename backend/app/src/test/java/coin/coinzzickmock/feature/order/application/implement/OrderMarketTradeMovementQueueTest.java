package coin.coinzzickmock.feature.order.application.implement;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.testsupport.TestTelemetryProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class OrderMarketTradeMovementQueueTest {
    @Test
    void registersQueueSizeGaugeAndTracksCurrentQueueDepth() {
        CapturingTelemetryProvider telemetryProvider = new CapturingTelemetryProvider();
        OrderMarketTradeMovementQueue queue = new OrderMarketTradeMovementQueue(
                2,
                new OrderMarketTradeMovementTelemetry(telemetryProvider)
        );

        assertThat(telemetryProvider.gaugeName).isEqualTo("market.trade.movement.queue.size.current");
        assertThat(telemetryProvider.gaugeValue.get().intValue()).isZero();

        assertThat(queue.enqueue(movement("2026-04-27T00:00:01Z"))).isTrue();

        assertThat(telemetryProvider.gaugeValue.get().intValue()).isEqualTo(1);
    }

    @Test
    void recordsQueueDropWhenBoundedQueueIsFull() {
        CapturingTelemetryProvider telemetryProvider = new CapturingTelemetryProvider();
        OrderMarketTradeMovementQueue queue = new OrderMarketTradeMovementQueue(
                1,
                new OrderMarketTradeMovementTelemetry(telemetryProvider)
        );

        assertThat(queue.enqueue(movement("2026-04-27T00:00:01Z"))).isTrue();
        assertThat(queue.enqueue(movement("2026-04-27T00:00:02Z"))).isFalse();

        assertThat(telemetryProvider.eventNames).containsExactly("market.trade.movement.queue.drop.total");
        assertThat(telemetryProvider.eventTags).containsExactly(Map.of("reason", "full"));
    }

    private static MarketTradePriceMovedEvent movement(String sourceEventTime) {
        return new MarketTradePriceMovedEvent(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse(sourceEventTime),
                Instant.parse(sourceEventTime)
        );
    }

    private static final class CapturingTelemetryProvider extends TestTelemetryProvider {
        private final List<String> eventNames = new ArrayList<>();
        private final List<Map<String, String>> eventTags = new ArrayList<>();
        private String gaugeName;
        private Supplier<Number> gaugeValue;

        @Override
        public void recordEvent(String eventName, Map<String, String> tags) {
            eventNames.add(eventName);
            eventTags.add(tags);
        }

        @Override
        public void registerGauge(String gaugeName, Map<String, String> tags, Supplier<Number> valueSupplier) {
            this.gaugeName = gaugeName;
            this.gaugeValue = valueSupplier;
        }
    }
}
