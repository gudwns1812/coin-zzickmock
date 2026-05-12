package coin.coinzzickmock.feature.position.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PositionSnapshotResultAssemblerTest {
    @Test
    void assemblesPositionResultWithAccountingManualCloseCapAndProtectivePrices() {
        PositionSnapshot remaining = PositionSnapshot.open(
                        "BTCUSDT",
                        "LONG",
                        "ISOLATED",
                        10,
                        1,
                        100,
                        100
                )
                .close(0.25, 120, 120, 0.0005)
                .remainingPosition();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository(remaining);
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, FuturesOrder.place(
                "manual-close",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.2,
                130.0,
                false,
                "MAKER",
                0,
                130
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "take-profit",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.75,
                140,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));

        PositionSnapshotResult result = assembler(positionRepository, orderRepository)
                .assemble(1L, remaining);

        assertEquals(0.25, result.accumulatedClosedQuantity(), 0.0001);
        assertEquals(0.2, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0.55, result.closeableQuantity(), 0.0001);
        assertEquals(140, result.takeProfitPrice(), 0.0001);
        assertEquals(null, result.stopLossPrice());
        assertEquals(4.985, result.realizedPnl(), 0.0001);
    }

    private PositionSnapshotResultAssembler assembler(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository
    ) {
        return new PositionSnapshotResultAssembler(
                positionRepository,
                orderRepository,
                new StaticAccountRepository(),
                new PendingCloseOrderCapReconciler(orderRepository),
                new LiquidationPolicy()
        );
    }

    private static class InMemoryPositionRepository extends coin.coinzzickmock.testsupport.TestPositionRepository {
        private final List<PositionSnapshot> positions;

        private InMemoryPositionRepository(PositionSnapshot position) {
            this.positions = List.of(position);
        }

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return positions;
        }
    }

    private static class InMemoryOrderRepository extends coin.coinzzickmock.testsupport.TestOrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.add(futuresOrder);
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return orders;
        }
    }

    private static class StaticAccountRepository extends coin.coinzzickmock.testsupport.TestAccountRepository {
        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.of(new TradingAccount(
                    memberId,
                    "demo@coinzzickmock.dev",
                    "Demo",
                    100_000,
                    100_000
            ));
        }
    }
}
