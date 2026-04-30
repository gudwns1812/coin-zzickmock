package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
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
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, accountRepository)
                .update("demo-member", "BTCUSDT", "LONG", "ISOLATED", 20);

        assertEquals(20, result.leverage());
        assertEquals(95, result.liquidationPrice(), 0.0001);
        assertEquals(100005, accountRepository.findByMemberId("demo-member").orElseThrow().availableMargin(), 0.0001);
        assertEquals(WalletHistorySource.TYPE_POSITION_LEVERAGE_CHANGE, accountRepository.lastSource.sourceType());
    }

    @Test
    void rejectsLeverageUpdateWhenPendingOpenOrderExistsForSameSide() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(100000);
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
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
                        .update("demo-member", "BTCUSDT", "LONG", "ISOLATED", 20));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void rejectsLeverageUpdateWhenAvailableMarginWouldGoNegative() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(1);
        positionRepository.save("demo-member", PositionSnapshot.open(
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
                        .update("demo-member", "BTCUSDT", "LONG", "ISOLATED", 1));

        assertEquals(ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN, thrown.errorCode());
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
                new PendingCloseOrderCapReconciler(orderRepository),
                new AfterCommitEventPublisher(event -> {
                })
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
            return List.copyOf(orders);
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
            throw new UnsupportedOperationException();
        }
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private TradingAccount account;
        private WalletHistorySource lastSource;

        private InMemoryAccountRepository(double availableMargin) {
            this.account = new TradingAccount(
                    "demo-member",
                    "demo@coinzzickmock.dev",
                    "Demo",
                    100000,
                    availableMargin
            );
        }

        @Override
        public Optional<TradingAccount> findByMemberId(String memberId) {
            return Optional.of(account);
        }

        @Override
        public TradingAccount save(TradingAccount account) {
            this.account = account;
            return account;
        }

        @Override
        public TradingAccount save(TradingAccount account, WalletHistorySource source) {
            this.lastSource = source;
            return save(account);
        }
    }
}
