package coin.coinzzickmock.feature.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovementPublisher;
import coin.coinzzickmock.feature.order.application.implement.OrderMarketTradeMovementQueue;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderMarketTradeMovementListenerTest {
    @Test
    void delegatesPublishedMovementToQueue() {
        OrderMarketTradeMovementQueue queue = mock(OrderMarketTradeMovementQueue.class);
        MarketTradePriceMovedEvent event = movement();
        when(queue.enqueue(event)).thenReturn(true);
        MarketTradePriceMovementPublisher listener = new OrderMarketTradeMovementListener(queue);

        assertThat(listener.publish(event)).isTrue();

        verify(queue).enqueue(event);
    }

    @Test
    void returnsFalseWhenQueueRejectsMovement() {
        OrderMarketTradeMovementQueue queue = mock(OrderMarketTradeMovementQueue.class);
        MarketTradePriceMovedEvent event = movement();
        when(queue.enqueue(event)).thenReturn(false);
        OrderMarketTradeMovementListener listener = new OrderMarketTradeMovementListener(queue);

        assertThat(listener.publish(event)).isFalse();
    }

    private static MarketTradePriceMovedEvent movement() {
        return new MarketTradePriceMovedEvent(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z"),
                Instant.parse("2026-04-27T00:00:01Z")
        );
    }
}
