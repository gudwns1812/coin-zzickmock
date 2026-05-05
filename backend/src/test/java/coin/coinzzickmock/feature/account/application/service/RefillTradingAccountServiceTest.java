package coin.coinzzickmock.feature.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository.LockedAccountRefillState;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.result.AccountRefillResult;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefillTradingAccountServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T15:01:00Z"), ZoneOffset.UTC);

    @Test
    void refillsWalletAndAvailableToInitialBalanceAndConsumesCount() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount(1L, "demo@coinzzickmock.dev", "Demo", 80_000, 70_000)
        );
        InMemoryRefillStateRepository stateRepository = new InMemoryRefillStateRepository();
        RefillTradingAccountService service = service(
                accountRepository,
                stateRepository,
                false,
                false
        );

        AccountRefillResult result = service.refill(1L);

        assertEquals(BigDecimal.valueOf(100_000.0), result.walletBalance());
        assertEquals(BigDecimal.valueOf(100_000.0), result.availableMargin());
        assertEquals(0, result.remainingCount());
        TradingAccount account = accountRepository.findByMemberId(1L).orElseThrow();
        assertEquals(100_000, account.walletBalance(), 0.0001);
        assertEquals(100_000, account.availableMargin(), 0.0001);
        assertEquals(0, stateRepository.state.remainingCount());
    }

    @Test
    void rejectsRefillWhenOpenPositionExists() {
        RefillTradingAccountService service = service(
                new InMemoryAccountRepository(new TradingAccount(1L, "demo@coinzzickmock.dev", "Demo", 80_000, 70_000)),
                new InMemoryRefillStateRepository(),
                true,
                false
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.refill(1L));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(ErrorCode.INVALID_REQUEST.message(), thrown.getMessage());
    }

    @Test
    void rejectsRefillWhenPendingOrderExists() {
        RefillTradingAccountService service = service(
                new InMemoryAccountRepository(new TradingAccount(1L, "demo@coinzzickmock.dev", "Demo", 80_000, 70_000)),
                new InMemoryRefillStateRepository(),
                false,
                true
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.refill(1L));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(ErrorCode.INVALID_REQUEST.message(), thrown.getMessage());
    }

    @Test
    void rejectsRefillWhenEitherBalanceIsAlreadyAtBaseline() {
        RefillTradingAccountService service = service(
                new InMemoryAccountRepository(new TradingAccount(1L, "demo@coinzzickmock.dev", "Demo", 80_000, 100_000)),
                new InMemoryRefillStateRepository(),
                false,
                false
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.refill(1L));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
        assertEquals(ErrorCode.INVALID_REQUEST.message(), thrown.getMessage());
    }

    private RefillTradingAccountService service(
            AccountRepository accountRepository,
            AccountRefillStateRepository stateRepository,
            boolean hasOpenPosition,
            boolean hasPendingOrder
    ) {
        return new RefillTradingAccountService(
                accountRepository,
                stateRepository,
                new GuardPositionRepository(hasOpenPosition),
                new GuardOrderRepository(hasPendingOrder),
                new AccountRefillDatePolicy(CLOCK),
                new AfterCommitEventPublisher(event -> {
                })
        );
    }

    private static class InMemoryRefillStateRepository implements AccountRefillStateRepository {
        private AccountRefillState state;

        @Override
        public Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate) {
            return Optional.ofNullable(state);
        }

        @Override
        public void provisionDailyStateIfAbsent(Long memberId, LocalDate refillDate) {
            if (state == null) {
                state = AccountRefillState.daily(memberId, refillDate);
            }
        }

        @Override
        public AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count) {
            if (state == null) {
                state = AccountRefillState.daily(memberId, refillDate);
            }
            state = state.addCount(count).withVersion(state.version() + 1);
            return state;
        }

        @Override
        public Optional<LockedAccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate) {
            return Optional.ofNullable(state).map(ignored -> new InMemoryLockedAccountRefillState());
        }

        private class InMemoryLockedAccountRefillState implements LockedAccountRefillState {
            @Override
            public AccountRefillState state() {
                return state;
            }

            @Override
            public AccountRefillState consumeOne() {
                state = state.consumeOne().withVersion(state.version() + 1);
                return state;
            }
        }
    }

    private static class InMemoryAccountRepository extends coin.coinzzickmock.testsupport.TestAccountRepository {
        private TradingAccount account;

        private InMemoryAccountRepository(TradingAccount account) {
            this.account = account;
        }

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.ofNullable(account);
        }

        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            return Optional.ofNullable(account);
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            this.account = account;
            return account;
        }

        @Override
        public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
            if (account.version() != expectedAccount.version()) {
                return AccountMutationResult.staleVersion(account);
            }
            account = nextAccount.withVersion(expectedAccount.version() + 1);
            return AccountMutationResult.updated(1, account);
        }
    }

    private record GuardPositionRepository(boolean hasOpenPosition) implements PositionRepository {
        @Override
        public boolean existsOpenByMemberId(Long memberId) {
            return hasOpenPosition;
        }

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return List.of();
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide, String marginMode) {
            return Optional.empty();
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide) {
            return Optional.empty();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return List.of();
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            return positionSnapshot;
        }

        @Override
        public PositionMutationResult updateWithVersion(
                Long memberId,
                PositionSnapshot expectedPosition,
                PositionSnapshot nextPosition
        ) {
            return PositionMutationResult.notFound();
        }

        @Override
        public PositionMutationResult deleteWithVersion(Long memberId, PositionSnapshot expectedPosition) {
            return PositionMutationResult.notFound();
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            return false;
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
        }
    }

    private record GuardOrderRepository(boolean hasPendingOrder) implements OrderRepository {
        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return List.of();
        }

        @Override
        public boolean existsPendingByMemberId(Long memberId) {
            return hasPendingOrder;
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
        public List<PendingOrderCandidate> findExecutablePendingLimitOrders(
                String symbol,
                double lowerPrice,
                double upperPrice,
                boolean sellSide
        ) {
            return List.of();
        }

        @Override
        public List<FuturesOrder> findPendingCloseOrders(
                Long memberId,
                String symbol,
                String positionSide,
                String marginMode
        ) {
            return List.of();
        }

        @Override
        public List<FuturesOrder> findPendingOpenOrders(Long memberId, String symbol, String positionSide) {
            return List.of();
        }

        @Override
        public List<FuturesOrder> findPendingConditionalCloseOrders(
                Long memberId,
                String symbol,
                String positionSide,
                String marginMode
        ) {
            return List.of();
        }

        @Override
        public List<FuturesOrder> findPendingConditionalCloseOrdersBySymbol(String symbol) {
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
        public Optional<FuturesOrder> claimPendingLimitFill(
                Long memberId,
                String orderId,
                double expectedLimitPrice,
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

        @Override
        public FuturesOrder updatePendingConditionalCloseOrder(
                Long memberId,
                String orderId,
                int leverage,
                double quantity,
                double triggerPrice,
                String ocoGroupId
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FuturesOrder> updatePendingLimitPrice(
                Long memberId,
                String orderId,
                double limitPrice,
                String feeType,
                double estimatedFee,
                double executionPrice
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int cancelPendingOrders(Long memberId, List<String> orderIds) {
            return 0;
        }

        @Override
        public boolean cancelPending(Long memberId, String orderId) {
            return false;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int capPendingOrderQuantity(Long memberId, List<String> orderIds, double maxQuantity) {
            return 0;
        }
    }
}
