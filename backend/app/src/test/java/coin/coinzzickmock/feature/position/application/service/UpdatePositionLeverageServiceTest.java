package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.dto.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.query.PositionSnapshotResultAssembler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePositionLeverageServiceTest {
    @Test
    void updatesPositionLeverageAndAdjustsAvailableMargin() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, accountRepository)
                .update(1L, "BTCUSDT", "LONG", "ISOLATED", 20);

        assertEquals(20, result.leverage());
        assertEquals(95.47738693467336, result.liquidationPrice(), 0.0001);
        assertEquals(100005, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void crossLeverageUpdateAppliesToBothSymbolSides() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ));
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "CROSS",
                10,
                2,
                100,
                100
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, accountRepository)
                .update(1L, "BTCUSDT", "LONG", "CROSS", 20);

        assertEquals(20, result.leverage());
        assertEquals(20, positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "CROSS")
                .orElseThrow()
                .leverage());
        assertEquals(20, positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "CROSS")
                .orElseThrow()
                .leverage());
        assertEquals(100015, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void isolatedLeverageUpdateAppliesToBothSymbolSides() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                2,
                100,
                100
        ));

        service(positionRepository, orderRepository, accountRepository)
                .update(1L, "BTCUSDT", "LONG", "ISOLATED", 20);

        assertEquals(20, positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .leverage());
        assertEquals(20, positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "ISOLATED")
                .orElseThrow()
                .leverage());
        assertEquals(100015, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void crossLeverageUpdateRejectsPendingOpenOrdersForEitherSide() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ));
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "CROSS",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "pending-short-open",
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                "CROSS",
                10,
                1,
                101.0,
                false,
                "MAKER",
                0,
                101
        ));

        CoreException thrown = assertThrows(CoreException.class, () ->
                service(positionRepository, orderRepository, accountRepository)
                        .update(1L, "BTCUSDT", "LONG", "CROSS", 20));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(10, positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "CROSS")
                .orElseThrow()
                .leverage());
        assertEquals(10, positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "CROSS")
                .orElseThrow()
                .leverage());
    }

    @Test
    void rejectsLeverageUpdateWhenPendingOpenOrderExistsForSameSide() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "pending-open",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "CROSS",
                10,
                1,
                99.0,
                false,
                "MAKER",
                0,
                99
        ));

        CoreException thrown = assertThrows(CoreException.class, () ->
                service(positionRepository, orderRepository, accountRepository)
                        .update(1L, "BTCUSDT", "LONG", "ISOLATED", 20));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void isolatedLeverageUpdateRejectsPendingOpenOrdersForEitherSide() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "pending-short-open",
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                "ISOLATED",
                10,
                1,
                101.0,
                false,
                "MAKER",
                0,
                101
        ));

        CoreException thrown = assertThrows(CoreException.class, () ->
                service(positionRepository, orderRepository, accountRepository)
                        .update(1L, "BTCUSDT", "LONG", "ISOLATED", 20));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(10, positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .leverage());
        assertEquals(10, positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "ISOLATED")
                .orElseThrow()
                .leverage());
    }

    @Test
    void rejectsLeverageUpdateWhenAvailableMarginWouldGoNegative() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(1);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                20,
                1,
                100,
                100
        ));

        CoreException thrown = assertThrows(CoreException.class, () ->
                service(positionRepository, orderRepository, accountRepository)
                        .update(1L, "BTCUSDT", "LONG", "ISOLATED", 1));

        assertEquals(ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN, thrown.errorCode());
    }

    @Test
    void crossLeverageResponseIncludesDynamicLiquidationEstimate() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(50, 100);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, accountRepository)
                .update(1L, "BTCUSDT", "LONG", "CROSS", 10);

        assertEquals(50.2512562814, result.liquidationPrice(), 0.0001);
        assertEquals("EXACT", result.liquidationPriceType());
    }

    @Test
    void leverageResponseCountsManualCloseOrdersButIgnoresTpslConditionalOrders() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "manual-close",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.25,
                120.0,
                false,
                "MAKER",
                0,
                120
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                130,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, accountRepository)
                .update(1L, "BTCUSDT", "LONG", "ISOLATED", 10);

        assertEquals(0.25, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0.75, result.closeableQuantity(), 0.0001);
        assertEquals(130, result.takeProfitPrice(), 0.0001);
    }

    private UpdatePositionLeverageService service(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository,
            InMemoryAccountRepository accountRepository
    ) {
        return new UpdatePositionLeverageService(
                positionRepository,
                orderRepository,
                accountRepository,
                new AfterCommitEventPublisher(event -> {
                }),
                new PositionSnapshotResultAssembler(
                        positionRepository,
                        orderRepository,
                        accountRepository,
                        new PendingCloseOrderCapReconciler(orderRepository),
                        new LiquidationPolicy()
                )
        ,
                new coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookWriter(new coin.coinzzickmock.feature.position.application.realtime.OpenPositionBook()));
    }

    private static class InMemoryPositionRepository extends coin.coinzzickmock.testsupport.TestPositionRepository {
        private final List<OpenPositionCandidate> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return positions.stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(OpenPositionCandidate::position)
                    .toList();
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(
                Long memberId,
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
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(new OpenPositionCandidate(memberId, positionSnapshot));
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            positions.removeIf(candidate -> candidate.memberId().equals(memberId)
                    && candidate.symbol().equals(symbol)
                    && candidate.positionSide().equals(positionSide)
                    && candidate.marginMode().equals(marginMode));
            return before != positions.size();
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
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
            return List.copyOf(orders);
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return orders.stream().filter(order -> order.orderId().equals(orderId)).findFirst();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.stream()
                    .filter(order -> order.symbol().equals(symbol))
                    .map(order -> new PendingOrderCandidate(1L, order))
                    .toList();
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

    private static class InMemoryAccountRepository extends coin.coinzzickmock.testsupport.TestAccountRepository {
        private TradingAccount account;

        private InMemoryAccountRepository(double availableMargin) {
            this(100000, availableMargin);
        }

        private InMemoryAccountRepository(double walletBalance, double availableMargin) {
            this.account = new TradingAccount(
                    1L,
                    "demo@coinzzickmock.dev",
                    "Demo",
                    walletBalance,
                    availableMargin
            );
        }

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.of(account);
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            if (this.account.memberId().equals(account.memberId())) {
                throw new IllegalStateException("account already exists");
            }
            this.account = account;
            return account;
        }

        @Override
        public AccountMutationResult updateWithVersion(
                TradingAccount expectedAccount,
                TradingAccount nextAccount
        ) {
            if (account.version() != expectedAccount.version()) {
                return AccountMutationResult.staleVersion(account);
            }
            account = nextAccount.withVersion(expectedAccount.version() + 1);
            return AccountMutationResult.updated(1, account);
        }
    }
}
