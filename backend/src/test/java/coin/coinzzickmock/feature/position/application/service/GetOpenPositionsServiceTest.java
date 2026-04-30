package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOpenPositionsServiceTest {
    @Test
    void marksPositionsAtReadTimeWithoutPersistingMarkOnlyMutation() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ).withVersion(3));
        GetOpenPositionsService service = new GetOpenPositionsService(
                positionRepository,
                new EmptyOrderRepository(),
                new PendingCloseOrderCapReconciler(new EmptyOrderRepository()),
                realtimePriceReader(110)
        );

        PositionSnapshotResult result = service.getPositions(1L).get(0);

        assertEquals(110, result.markPrice(), 0.0001);
        assertEquals(20, result.unrealizedPnl(), 0.0001);
        assertEquals(0, result.accumulatedClosedQuantity(), 0.0001);
        assertEquals(0, result.pendingCloseQuantity(), 0.0001);
        assertEquals(2, result.closeableQuantity(), 0.0001);
        assertEquals(3, positionRepository.position.version());
        assertEquals(100, positionRepository.position.markPrice(), 0.0001);
    }

    @Test
    void includesAccumulatedClosedQuantityForPartiallyClosedRemainingPosition() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1.25,
                100,
                100,
                90.0,
                0,
                Instant.now(),
                2,
                0.75,
                82.5,
                7.5,
                0.04
        ));
        GetOpenPositionsService service = new GetOpenPositionsService(
                positionRepository,
                new EmptyOrderRepository(),
                new PendingCloseOrderCapReconciler(new EmptyOrderRepository()),
                realtimePriceReader(100)
        );

        PositionSnapshotResult result = service.getPositions(1L).get(0);

        assertEquals(0.75, result.accumulatedClosedQuantity(), 0.0001);
        assertEquals(0, result.pendingCloseQuantity(), 0.0001);
        assertEquals(1.25, result.closeableQuantity(), 0.0001);
    }

    @Test
    void includesPendingCloseAndCloseableQuantity() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        PendingCloseOrderRepository orderRepository = new PendingCloseOrderRepository();
        GetOpenPositionsService service = new GetOpenPositionsService(
                positionRepository,
                orderRepository,
                new PendingCloseOrderCapReconciler(orderRepository),
                realtimePriceReader(100)
        );

        PositionSnapshotResult result = service.getPositions(1L).get(0);

        assertEquals(0, result.accumulatedClosedQuantity(), 0.0001);
        assertEquals(0.4, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0.6, result.closeableQuantity(), 0.0001);
    }

    @Test
    void countsTpslOcoGroupAsSingleCloseExposure() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        TpslOcoOrderRepository orderRepository = new TpslOcoOrderRepository();
        GetOpenPositionsService service = new GetOpenPositionsService(
                positionRepository,
                orderRepository,
                new PendingCloseOrderCapReconciler(orderRepository),
                realtimePriceReader(100)
        );

        PositionSnapshotResult result = service.getPositions(1L).get(0);

        assertEquals(1, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0, result.closeableQuantity(), 0.0001);
        assertEquals(110, result.takeProfitPrice(), 0.0001);
        assertEquals(95, result.stopLossPrice(), 0.0001);
    }


    @Test
    void partiallyClosedRemainingPositionIncludesAccumulatedClosedQuantity() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1.5,
                100,
                100,
                90.0,
                0,
                java.time.Instant.now(),
                2.0,
                0.5,
                55.0,
                5.0,
                0,
                0.02,
                0,
                null,
                null,
                0
        ));
        GetOpenPositionsService service = new GetOpenPositionsService(
                positionRepository,
                new EmptyOrderRepository(),
                new PendingCloseOrderCapReconciler(new EmptyOrderRepository()),
                realtimePriceReader(100)
        );

        PositionSnapshotResult result = service.getPositions(1L).get(0);

        assertEquals(0.5, result.accumulatedClosedQuantity(), 0.0001);
        assertEquals(0, result.pendingCloseQuantity(), 0.0001);
        assertEquals(1.5, result.closeableQuantity(), 0.0001);
    }

    private static class ReadOnlyPositionRepository implements PositionRepository {
        private final PositionSnapshot position;

        private ReadOnlyPositionRepository(PositionSnapshot position) {
            this.position = position;
        }

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return List.of(position);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide, String marginMode) {
            return Optional.of(position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return List.of(new OpenPositionCandidate(1L, position));
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            fail("read-time mark-to-market must not save positions");
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
            return false;
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
        }
    }

    private static class EmptyOrderRepository implements OrderRepository {
        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return List.of();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return Optional.empty();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return List.of();
        }

        @Override
        public Optional<FuturesOrder> claimPendingFill(
                Long memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return Optional.empty();
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            throw new UnsupportedOperationException();
        }
    }

    private static class PendingCloseOrderRepository extends EmptyOrderRepository {
        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return List.of(FuturesOrder.place(
                    "close-order",
                    "BTCUSDT",
                    "LONG",
                    "LIMIT",
                    FuturesOrder.PURPOSE_CLOSE_POSITION,
                    "ISOLATED",
                    10,
                    0.4,
                    120.0,
                    false,
                    "MAKER",
                    0,
                    120
            ));
        }
    }

    private static class TpslOcoOrderRepository extends EmptyOrderRepository {
        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return List.of(
                    FuturesOrder.conditionalClose(
                            "tp",
                            "BTCUSDT",
                            "LONG",
                            "ISOLATED",
                            10,
                            1,
                            110,
                            FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                            "oco-1"
                    ),
                    FuturesOrder.conditionalClose(
                            "sl",
                            "BTCUSDT",
                            "LONG",
                            "ISOLATED",
                            10,
                            1,
                            95,
                            FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                            "oco-1"
                    )
            );
        }
    }

    private static RealtimeMarketPriceReader realtimePriceReader(double markPrice) {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        Instant now = Instant.now();
        store.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-" + markPrice,
                BigDecimal.valueOf(markPrice),
                BigDecimal.ONE,
                "buy",
                now,
                now
        ));
        store.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(0.0001),
                now.plusSeconds(3600),
                now,
                now
        ));
        return new RealtimeMarketPriceReader(store);
    }
}
