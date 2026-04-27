package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
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
                new FakeProviders(110)
        );

        PositionSnapshotResult result = service.getPositions("demo-member").get(0);

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
                new FakeProviders(100)
        );

        PositionSnapshotResult result = service.getPositions("demo-member").get(0);

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
                new FakeProviders(100)
        );

        PositionSnapshotResult result = service.getPositions("demo-member").get(0);

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
                new FakeProviders(100)
        );

        PositionSnapshotResult result = service.getPositions("demo-member").get(0);

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
                new FakeProviders(100)
        );

        PositionSnapshotResult result = service.getPositions("demo-member").get(0);

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
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return List.of(position);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(String memberId, String symbol, String positionSide, String marginMode) {
            return Optional.of(position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return List.of(new OpenPositionCandidate("demo-member", position));
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            fail("read-time mark-to-market must not save positions");
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
            return false;
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
        }
    }

    private static class EmptyOrderRepository implements OrderRepository {
        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return List.of();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return Optional.empty();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return List.of();
        }

        @Override
        public Optional<FuturesOrder> claimPendingFill(
                String memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return Optional.empty();
        }

        @Override
        public FuturesOrder updateStatus(String memberId, String orderId, String status) {
            throw new UnsupportedOperationException();
        }
    }

    private static class PendingCloseOrderRepository extends EmptyOrderRepository {
        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
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
        public List<FuturesOrder> findByMemberId(String memberId) {
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

    private record FakeProviders(double markPrice) implements Providers {
        @Override
        public AuthProvider auth() {
            return new AuthProvider() {
                @Override
                public Actor currentActor() {
                    return new Actor("demo-member", "demo@coinzzickmock.dev", "Demo");
                }

                @Override
                public boolean isAuthenticated() {
                    return true;
                }
            };
        }

        @Override
        public ConnectorProvider connector() {
            return () -> new MarketDataGateway() {
                @Override
                public List<MarketSnapshot> loadSupportedMarkets() {
                    return List.of(loadMarket("BTCUSDT"));
                }

                @Override
                public MarketSnapshot loadMarket(String symbol) {
                    return new MarketSnapshot(symbol, "Bitcoin Perpetual", markPrice, markPrice, markPrice, 0.0001, 0.1);
                }
            };
        }

        @Override
        public TelemetryProvider telemetry() {
            return new TelemetryProvider() {
                @Override
                public void recordUseCase(String useCaseName) {
                }

                @Override
                public void recordFailure(String useCaseName, String reason) {
                }
            };
        }

        @Override
        public FeatureFlagProvider featureFlags() {
            return key -> true;
        }
    }
}
