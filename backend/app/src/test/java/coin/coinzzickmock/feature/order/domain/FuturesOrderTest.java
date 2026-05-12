package coin.coinzzickmock.feature.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuturesOrderTest {
    @Test
    void marksExecutableOrderAsFilled() {
        FuturesOrder order = FuturesOrder.place(
                "order-1",
                "BTCUSDT",
                "LONG",
                "MARKET",
                "cross",
                10,
                0.25,
                null,
                true,
                "TAKER",
                12.5,
                50000
        );

        assertEquals("FILLED", order.status());
    }

    @Test
    void marksNonExecutableOrderAsPending() {
        FuturesOrder order = FuturesOrder.place(
                "order-2",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "cross",
                10,
                0.25,
                49000.0,
                false,
                "MAKER",
                4.5,
                50000
        );

        assertEquals("PENDING", order.status());
    }

    @Test
    void withLimitPriceChangesPriceAndKeepsOrderIdentity() {
        FuturesOrder order = FuturesOrder.place(
                "order-3",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "cross",
                10,
                0.25,
                49000.0,
                false,
                "MAKER",
                4.5,
                49000
        );

        FuturesOrder updated = order.withLimitPrice(48500, "MAKER", 1.81875, 48500);

        assertEquals("order-3", updated.orderId());
        assertEquals("PENDING", updated.status());
        assertEquals(48500, updated.limitPrice(), 0.0001);
        assertEquals(1.81875, updated.estimatedFee(), 0.0001);
        assertEquals(48500, updated.executionPrice(), 0.0001);
        assertEquals(order.orderTime(), updated.orderTime());
    }
}
