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
}
