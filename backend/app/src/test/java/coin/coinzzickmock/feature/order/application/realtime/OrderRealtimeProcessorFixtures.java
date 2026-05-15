package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.service.AccountOrderMutationLock;
import coin.coinzzickmock.feature.order.application.service.FilledOpenOrderApplier;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBook;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookHydrator;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookWriter;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.ApplicationEventPublisher;

final class OrderRealtimeProcessorFixtures {
    private OrderRealtimeProcessorFixtures() {
    }

    static Scenario scenario() {
        return new Scenario();
    }

    static final class Scenario {
        InMemoryOrderRepository orders = new InMemoryOrderRepository();
        final InMemoryPositionRepository positions = new InMemoryPositionRepository();
        final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        final CapturingEventPublisher events = new CapturingEventPublisher();
        final InMemoryPositionHistoryRepository histories = new InMemoryPositionHistoryRepository();
        final InMemoryRewardPointRepository rewardPoints = new InMemoryRewardPointRepository();
        final RealtimeMarketDataStore realtimeMarketDataStore = new RealtimeMarketDataStore();
        final PendingLimitOrderBook pendingLimitOrderBook = new PendingLimitOrderBook();
        final OpenPositionBook openPositionBook = new OpenPositionBook();
        final OpenPositionBookHydrator openPositionBookHydrator = new OpenPositionBookHydrator(positions, openPositionBook);
        final OpenPositionBookWriter openPositionBookWriter = new OpenPositionBookWriter(openPositionBook);
        private final AtomicInteger tradeSequence = new AtomicInteger();

        PendingOrderFillProcessor pendingFillProcessor() {
            return pendingFillProcessor(new TrackingPendingOrderExecutionCache());
        }

        PendingOrderFillProcessor pendingFillProcessor(PendingOrderExecutionCache cache) {
            AfterCommitEventPublisher afterCommitEventPublisher = afterCommitEventPublisher();
            return new PendingOrderFillProcessor(
                    orders,
                    positions,
                    cache,
                    pendingLimitOrderBook,
                    positionCloseFinalizer(afterCommitEventPublisher),
                    pendingCloseOrderCapReconciler(),
                    new StaleProtectiveCloseOrderCanceller(orders),
                    afterCommitEventPublisher,
                    realtimePriceReader(),
                    new FilledOpenOrderApplier(ordersAwareAccountRepository(), positions, afterCommitEventPublisher, openPositionBookWriter),
                    mutationLock()
            );
        }

        PositionLiquidationProcessor liquidationProcessor() {
            return liquidationProcessor(new StaleProtectiveCloseOrderCanceller(orders));
        }

        PositionLiquidationProcessor liquidationProcessor(StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller) {
            return liquidationProcessor(staleProtectiveCloseOrderCanceller, openPositionBookHydrator);
        }

        PositionLiquidationProcessor liquidationProcessor(OpenPositionBookHydrator openPositionBookHydrator) {
            return liquidationProcessor(new StaleProtectiveCloseOrderCanceller(orders), openPositionBookHydrator);
        }

        private PositionLiquidationProcessor liquidationProcessor(
                StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller,
                OpenPositionBookHydrator openPositionBookHydrator
        ) {
            AfterCommitEventPublisher afterCommitEventPublisher = afterCommitEventPublisher();
            return new PositionLiquidationProcessor(
                    positions,
                    accounts,
                    new LiquidationPolicy(),
                    positionCloseFinalizer(afterCommitEventPublisher),
                    pendingCloseOrderCapReconciler(),
                    staleProtectiveCloseOrderCanceller,
                    afterCommitEventPublisher,
                    realtimePriceReader(),
                    mutationLock(),
                    openPositionBook,
                    openPositionBookHydrator
            );
        }

        PositionTakeProfitStopLossProcessor takeProfitStopLossProcessor() {
            AfterCommitEventPublisher afterCommitEventPublisher = afterCommitEventPublisher();
            return new PositionTakeProfitStopLossProcessor(
                    orders,
                    positions,
                    positionCloseFinalizer(afterCommitEventPublisher),
                    pendingCloseOrderCapReconciler(),
                    new StaleProtectiveCloseOrderCanceller(orders),
                    afterCommitEventPublisher,
                    realtimePriceReader(),
                    mutationLock()
            );
        }

        MarketSummaryResult market(double lastPrice, double markPrice) {
            seedRealtimeMarket(lastPrice, markPrice);
            return rawMarket(lastPrice, markPrice);
        }

        MarketSummaryResult rawMarket(double lastPrice, double markPrice) {
            return new MarketSummaryResult(
                    "BTCUSDT",
                    "Bitcoin Perpetual",
                    lastPrice,
                    markPrice,
                    markPrice,
                    0.0001,
                    0.1
            );
        }

        MarketSummaryUpdatedEvent marketEvent(double previousLastPrice, double lastPrice, double markPrice) {
            return MarketSummaryUpdatedEvent.from(market(previousLastPrice, previousLastPrice), market(lastPrice, markPrice));
        }

        FuturesOrder pendingOpenOrderAt(String orderId, String positionSide, double limitPrice, Instant orderTime) {
            return new FuturesOrder(
                    orderId,
                    "BTCUSDT",
                    positionSide,
                    FuturesOrder.TYPE_LIMIT,
                    FuturesOrder.PURPOSE_OPEN_POSITION,
                    "ISOLATED",
                    10,
                    0.1,
                    limitPrice,
                    FuturesOrder.STATUS_PENDING,
                    "MAKER",
                    0,
                    limitPrice,
                    orderTime
            );
        }

        FuturesOrder pendingCloseOrderAt(
                String orderId,
                String positionSide,
                double quantity,
                double limitPrice,
                Instant orderTime
        ) {
            return new FuturesOrder(
                    orderId,
                    "BTCUSDT",
                    positionSide,
                    FuturesOrder.TYPE_LIMIT,
                    FuturesOrder.PURPOSE_CLOSE_POSITION,
                    "ISOLATED",
                    10,
                    quantity,
                    limitPrice,
                    FuturesOrder.STATUS_PENDING,
                    "MAKER",
                    0,
                    limitPrice,
                    orderTime
            );
        }

        PositionSnapshot openPosition(String positionSide, String marginMode, double quantity, double entryPrice) {
            return PositionSnapshot.open(
                    "BTCUSDT",
                    positionSide,
                    marginMode,
                    10,
                    quantity,
                    entryPrice,
                    entryPrice
            );
        }

        private AccountRepository ordersAwareAccountRepository() {
            return accounts;
        }

        private AfterCommitEventPublisher afterCommitEventPublisher() {
            return new AfterCommitEventPublisher(events);
        }

        PositionCloseFinalizer positionCloseFinalizerForTest() {
            return positionCloseFinalizer(afterCommitEventPublisher());
        }

        private PositionCloseFinalizer positionCloseFinalizer(AfterCommitEventPublisher afterCommitEventPublisher) {
            return new PositionCloseFinalizer(
                    positions,
                    accounts,
                    histories,
                    new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPoints),
                    afterCommitEventPublisher,
                    openPositionBookWriter
            );
        }

        private PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler() {
            return new PendingCloseOrderCapReconciler(orders);
        }

        private AccountOrderMutationLock mutationLock() {
            return new AccountOrderMutationLock(accounts);
        }

        private RealtimeMarketPriceReader realtimePriceReader() {
            return new RealtimeMarketPriceReader(realtimeMarketDataStore);
        }

        private void seedRealtimeMarket(double lastPrice, double markPrice) {
            Instant now = Instant.now();
            realtimeMarketDataStore.acceptTrade(new RealtimeMarketTradeTick(
                    "BTCUSDT",
                    "trade-" + tradeSequence.incrementAndGet(),
                    BigDecimal.valueOf(lastPrice),
                    BigDecimal.ONE,
                    "buy",
                    now,
                    now
            ));
            realtimeMarketDataStore.acceptTicker(new RealtimeMarketTickerUpdate(
                    "BTCUSDT",
                    BigDecimal.valueOf(lastPrice),
                    BigDecimal.valueOf(markPrice),
                    BigDecimal.valueOf(markPrice),
                    BigDecimal.valueOf(0.0001),
                    now.plusSeconds(3600),
                    now,
                    now
            ));
        }
    }

    static final class TrackingPendingOrderExecutionCache extends PendingOrderExecutionCache {
        private final List<String> evictedOrderIds = new ArrayList<>();

        @Override
        public void evict(String symbol, Long memberId, String orderId) {
            evictedOrderIds.add(orderId);
            super.evict(symbol, memberId, orderId);
        }

        List<String> evictedOrderIds() {
            return List.copyOf(evictedOrderIds);
        }
    }

    static final class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<TradingExecutionEvent> tradingEvents = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof TradingExecutionEvent tradingExecutionEvent) {
                tradingEvents.add(tradingExecutionEvent);
            }
        }

        List<TradingExecutionEvent> tradingEvents() {
            return List.copyOf(tradingEvents);
        }
    }

    static class InMemoryOrderRepository extends coin.coinzzickmock.testsupport.TestOrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();
        private final List<String> limitClaimAttempts = new ArrayList<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.put(key(memberId, futuresOrder.orderId()), new PendingOrderCandidate(memberId, futuresOrder));
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return orders.values().stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(PendingOrderCandidate::order)
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return Optional.ofNullable(orders.get(key(memberId, orderId)))
                    .map(PendingOrderCandidate::order);
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.values().stream()
                    .filter(candidate -> candidate.symbol().equalsIgnoreCase(symbol))
                    .filter(candidate -> candidate.order().isPending())
                    .toList();
        }

        @Override
        public List<PendingOrderCandidate> findPendingNonConditionalLimitOrders() {
            return orders.values().stream()
                    .filter(candidate -> candidate.order().isPending())
                    .filter(candidate -> !candidate.order().isConditionalOrder())
                    .filter(candidate -> FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(candidate.order().orderType()))
                    .filter(candidate -> candidate.order().limitPrice() != null)
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
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            if (candidate == null || !candidate.order().isPending()) {
                return Optional.empty();
            }
            FuturesOrder filled = candidate.order().fill(executionPrice, feeType, estimatedFee);
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, filled));
            return Optional.of(filled);
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
            limitClaimAttempts.add(orderId + ":" + expectedLimitPrice + ":" + executionPrice);
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            if (candidate == null || !candidate.order().isPending()) {
                return Optional.empty();
            }
            if (!FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(candidate.order().orderType())
                    || candidate.order().isConditionalOrder()
                    || candidate.order().limitPrice() == null
                    || Double.compare(candidate.order().limitPrice(), expectedLimitPrice) != 0) {
                return Optional.empty();
            }
            FuturesOrder filled = candidate.order().fill(executionPrice, feeType, estimatedFee);
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, filled));
            return Optional.of(filled);
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = switch (status) {
                case FuturesOrder.STATUS_CANCELLED -> candidate.order().cancel();
                case FuturesOrder.STATUS_FILLED -> candidate.order().fill(
                        candidate.order().executionPrice(),
                        candidate.order().feeType(),
                        candidate.order().estimatedFee()
                );
                case FuturesOrder.STATUS_PENDING -> candidate.order();
                default -> throw new IllegalArgumentException("Unsupported order status: " + status);
            };
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = candidate.order().withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                updated = updated.cancel();
            }
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        List<String> limitClaimAttempts() {
            return List.copyOf(limitClaimAttempts);
        }

        private String key(Long memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }

    static final class InMemoryPositionRepository extends coin.coinzzickmock.testsupport.TestPositionRepository {
        private final Map<String, OpenPositionCandidate> positions = new LinkedHashMap<>();
        int findOpenBySymbolCalls;


        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return positions.values().stream()
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
            return Optional.ofNullable(positions.get(key(memberId, symbol, positionSide, marginMode)))
                    .map(OpenPositionCandidate::position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            findOpenBySymbolCalls++;
            return positions.values().stream()
                    .filter(candidate -> candidate.symbol().equalsIgnoreCase(symbol))
                    .toList();
        }

        @Override
        public List<OpenPositionCandidate> findAllOpenCandidates() {
            return List.copyOf(positions.values());
        }

        int findOpenBySymbolCalls() {
            return findOpenBySymbolCalls;
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            positions.put(
                    key(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode()),
                    new OpenPositionCandidate(memberId, positionSnapshot)
            );
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            return positions.remove(key(memberId, symbol, positionSide, marginMode)) != null;
        }

        private String key(Long memberId, String symbol, String positionSide, String marginMode) {
            return memberId + ":" + symbol + ":" + positionSide + ":" + marginMode;
        }
    }

    static final class InMemoryAccountRepository extends coin.coinzzickmock.testsupport.TestAccountRepository {
        private final Map<Long, TradingAccount> accounts = new LinkedHashMap<>();

        InMemoryAccountRepository() {
            accounts.put(1L, new TradingAccount(
                    1L,
                    "demo@coinzzickmock.dev",
                    "Demo",
                    100000,
                    100000
            ));
        }

        void put(Long memberId, TradingAccount account) {
            accounts.put(memberId, account);
        }

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.ofNullable(accounts.get(memberId));
        }

        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            return Optional.ofNullable(accounts.get(memberId));
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            if (accounts.containsKey(account.memberId())) {
                throw new IllegalStateException("account already exists");
            }
            accounts.put(account.memberId(), account);
            return account;
        }

        @Override
        public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
            TradingAccount current = accounts.get(expectedAccount.memberId());
            if (current == null) {
                return AccountMutationResult.notFound();
            }
            if (current.version() != expectedAccount.version()) {
                return AccountMutationResult.staleVersion(current);
            }
            TradingAccount saved = nextAccount.withVersion(expectedAccount.version() + 1);
            accounts.put(saved.memberId(), saved);
            return AccountMutationResult.updated(1, saved);
        }
    }

    static final class InMemoryPositionHistoryRepository implements PositionHistoryRepository {
        private final Map<Long, List<PositionHistory>> histories = new LinkedHashMap<>();

        @Override
        public PositionHistory save(Long memberId, PositionHistory positionHistory) {
            histories.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(positionHistory);
            return positionHistory;
        }

        @Override
        public List<PositionHistory> findByMemberId(Long memberId, String symbol) {
            return histories.getOrDefault(memberId, List.of()).stream()
                    .filter(history -> symbol == null || history.symbol().equals(symbol))
                    .toList();
        }
    }

    static final class InMemoryRewardPointRepository extends coin.coinzzickmock.testsupport.TestRewardPointRepository {
        private RewardPointWallet wallet = new RewardPointWallet(1L, 0);

        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return wallet.memberId().equals(memberId) ? Optional.of(wallet) : Optional.empty();
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            wallet = rewardPointWallet;
            return rewardPointWallet;
        }

        RewardPointWallet wallet() {
            return wallet;
        }
    }
}
