package coin.coinzzickmock.feature.order.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.testsupport.TestOrderRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PendingLimitOrderBookHydratorTest {
    @Test
    void startHydratesPersistedPendingLimitOrdersBeforeMovementWorkerPhase() {
        PendingLimitOrderBook orderBook = new PendingLimitOrderBook();
        PendingLimitOrderBookHydrator hydrator = new PendingLimitOrderBookHydrator(
                repositoryReturning(
                        order("open-long", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, null),
                        order("conditional-with-limit", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, 98d)
                ),
                orderBook
        );

        hydrator.start();

        assertThat(hydrator.isRunning()).isTrue();
        assertThat(hydrator.getPhase()).isLessThan(MarketTradeMovementWorker.PHASE);
        assertThat(orderBook.executableCandidates(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z")
        )).extracting(PendingOrderCandidate::orderId)
                .containsExactly("open-long");
    }

    @Test
    void stopMarksHydratorNotRunningWithoutClearingAlreadyHydratedBook() {
        PendingLimitOrderBook orderBook = new PendingLimitOrderBook();
        PendingLimitOrderBookHydrator hydrator = new PendingLimitOrderBookHydrator(
                repositoryReturning(order("open-long", "LONG", FuturesOrder.PURPOSE_OPEN_POSITION, 99, null)),
                orderBook
        );

        hydrator.start();
        hydrator.stop();

        assertThat(hydrator.isRunning()).isFalse();
        assertThat(orderBook.size()).isEqualTo(1);
    }

    private TestOrderRepository repositoryReturning(FuturesOrder... orders) {
        return new TestOrderRepository() {
            @Override
            public List<PendingOrderCandidate> findPendingNonConditionalLimitOrders() {
                return Arrays.stream(orders)
                        .map(order -> new PendingOrderCandidate(1L, order))
                        .toList();
            }
        };
    }

    private FuturesOrder order(
            String orderId,
            String positionSide,
            String orderPurpose,
            double limitPrice,
            Double triggerPrice
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
                Instant.parse("2026-04-27T00:00:00Z"),
                triggerPrice,
                triggerPrice == null ? null : FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                triggerPrice == null ? null : FuturesOrder.TRIGGER_SOURCE_MARK_PRICE,
                null
        );
    }
}
