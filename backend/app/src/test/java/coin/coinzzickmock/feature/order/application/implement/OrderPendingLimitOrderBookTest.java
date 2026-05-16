package coin.coinzzickmock.feature.order.application.implement;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderPendingLimitOrderBookTest {
    @Test
    void returnsBuySideCandidatesInsideDownMoveInFillOrder() {
        OrderPendingLimitOrderBook orderBook = new OrderPendingLimitOrderBook();
        orderBook.add(1L, order("long-99", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:02Z"));
        orderBook.add(1L, order("long-100", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 100, "2026-04-27T00:00:01Z"));
        orderBook.add(1L, order("short-99", "SHORT", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:00Z"));
        orderBook.add(1L, order("outside", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 97, "2026-04-27T00:00:00Z"));

        assertThat(orderBook.executableCandidates("BTCUSDT", 101, 98, MarketPriceMovementDirection.DOWN))
                .extracting(candidate -> candidate.orderId())
                .containsExactly("long-100", "long-99");
    }

    @Test
    void returnsSellSideCandidatesInsideUpMoveInFillOrder() {
        OrderPendingLimitOrderBook orderBook = new OrderPendingLimitOrderBook();
        orderBook.add(1L, order("short-101", "SHORT", FuturesOrder.PURPOSE_OPEN_POSITION, 101, "2026-04-27T00:00:02Z"));
        orderBook.add(1L, order("close-long-100", "LONG", FuturesOrder.PURPOSE_CLOSE_POSITION, 100, "2026-04-27T00:00:01Z"));
        orderBook.add(1L, order("long-100", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 100, "2026-04-27T00:00:00Z"));

        assertThat(orderBook.executableCandidates("BTCUSDT", 99, 102, MarketPriceMovementDirection.UP))
                .extracting(candidate -> candidate.orderId())
                .containsExactly("close-long-100", "short-101");
    }

    @Test
    void replaceAndRemoveKeepIndexCurrent() {
        OrderPendingLimitOrderBook orderBook = new OrderPendingLimitOrderBook();
        orderBook.add(1L, order("long", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 97, "2026-04-27T00:00:00Z"));
        orderBook.replace(1L, order("long", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:00Z"));

        assertThat(orderBook.executableCandidates("BTCUSDT", 101, 98, MarketPriceMovementDirection.DOWN))
                .extracting(candidate -> candidate.orderId())
                .containsExactly("long");

        orderBook.remove(1L, "long");

        assertThat(orderBook.executableCandidates("BTCUSDT", 101, 98, MarketPriceMovementDirection.DOWN))
                .isEmpty();
    }

    @Test
    void excludesOrdersCreatedAfterMovementWasReceived() {
        OrderPendingLimitOrderBook orderBook = new OrderPendingLimitOrderBook();
        orderBook.add(1L, order("before", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:00Z"));
        orderBook.add(1L, order("after", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:02Z"));

        assertThat(orderBook.executableCandidates(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z")
        )).extracting(candidate -> candidate.orderId())
                .containsExactly("before");
    }

    @Test
    void replaceAfterCommitUsesReplacementTimeAsEffectiveTime() {
        OrderPendingLimitOrderBook orderBook = new OrderPendingLimitOrderBook();
        FuturesOrder edited = order("edited", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, "2026-04-27T00:00:00Z");

        orderBook.replaceAfterCommit(1L, edited);

        assertThat(orderBook.executableCandidates(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z")
        )).isEmpty();
    }

    private FuturesOrder order(
            String orderId,
            String positionSide,
            String orderPurpose,
            double limitPrice,
            String orderTime
    ) {
        return new FuturesOrder(
                orderId,
                "BTCUSDT",
                positionSide,
                FuturesOrder.TYPE_LIMIT,
                orderPurpose,
                "ISOLATED",
                10,
                0.1,
                limitPrice,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                0,
                limitPrice,
                Instant.parse(orderTime)
        );
    }
}
