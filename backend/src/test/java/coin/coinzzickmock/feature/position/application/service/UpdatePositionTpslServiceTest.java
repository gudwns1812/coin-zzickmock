package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePositionTpslServiceTest {
    @Test
    void createsTakeProfitAndStopLossOrdersWhenPricesAreNotAlreadyBreached() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                110.0,
                95.0
        );

        assertEquals(110, result.takeProfitPrice(), 0.0001);
        assertEquals(95, result.stopLossPrice(), 0.0001);
        PositionSnapshot persisted = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(null, persisted.takeProfitPrice());
        assertEquals(null, persisted.stopLossPrice());
        assertEquals(0, persisted.version());
        List<FuturesOrder> orders = orderRepository.findPendingConditionalCloseOrders(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "ISOLATED"
        );
        assertEquals(2, orders.size());
        assertEquals(1, orders.stream().map(FuturesOrder::ocoGroupId).distinct().count());
    }

    @Test
    void rejectsAlreadyBreachedLongTakeProfit() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        assertThrows(CoreException.class, () -> service(positionRepository, new InMemoryOrderRepository(), 101).update(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                100.0,
                null
        ));
    }

    @Test
    void rejectsAlreadyBreachedShortStopLoss() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        assertThrows(CoreException.class, () -> service(positionRepository, new InMemoryOrderRepository(), 101).update(
                "demo-member",
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                null,
                100.0
        ));
    }

    private UpdatePositionTpslService service(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository,
            double markPrice
    ) {
        return new UpdatePositionTpslService(
                positionRepository,
                orderRepository,
                new PendingCloseOrderCapReconciler(orderRepository),
                new FakeProviders(markPrice)
        );
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final List<OpenPositionCandidate> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return positions.stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(OpenPositionCandidate::position)
                    .toList();
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(
                String memberId,
                String symbol,
                String positionSide,
                String marginMode
        ) {
            return positions.stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(OpenPositionCandidate::position)
                    .filter(position -> position.symbol().equals(symbol))
                    .filter(position -> position.positionSide().equals(positionSide))
                    .filter(position -> position.marginMode().equals(marginMode))
                    .findFirst();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .toList();
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(new OpenPositionCandidate(memberId, positionSnapshot));
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            positions.removeIf(candidate -> candidate.memberId().equals(memberId)
                    && candidate.symbol().equals(symbol)
                    && candidate.positionSide().equals(positionSide)
                    && candidate.marginMode().equals(marginMode));
            return before != positions.size();
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
        }
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            orders.add(futuresOrder);
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return orders;
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return orders.stream().filter(order -> order.orderId().equals(orderId)).findFirst();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.stream()
                    .filter(order -> order.symbol().equals(symbol))
                    .map(order -> new PendingOrderCandidate("demo-member", order))
                    .toList();
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
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            orders.remove(order);
            FuturesOrder updated = FuturesOrder.STATUS_CANCELLED.equals(status) ? order.cancel() : order;
            orders.add(updated);
            return updated;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(String memberId, String orderId, double quantity, String status) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            orders.remove(order);
            FuturesOrder updated = order.withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equals(status)) {
                updated = updated.cancel();
            }
            orders.add(updated);
            return updated;
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
            return () -> new FakeMarketDataGateway(markPrice);
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
            return flag -> true;
        }
    }

    private record FakeMarketDataGateway(double markPrice) implements MarketDataGateway {
        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of(loadMarket("BTCUSDT"));
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return new MarketSnapshot(symbol, symbol, markPrice, markPrice, markPrice, 0, 0);
        }
    }
}
