package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.realtime.PendingOrderExecutionCache;
import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.service.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class MarketOrderExecutionServiceTest {
    @Test
    void fillsExecutablePendingOrderFromMarketEventAndPublishesSignal() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save("demo-member", new FuturesOrder(
                "order-1",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99.0,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                0,
                99
        ));

        MarketOrderExecutionService service = service(
                orderRepository,
                positionRepository,
                accountRepository,
                eventPublisher
        );

        service.onMarketUpdated(new MarketSummaryUpdatedEvent(market(98, 98)));

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId("demo-member", "order-1").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(98, filled.executionPrice(), 0.0001);
        assertTrue(positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED").isPresent());
        assertEquals("ORDER_FILLED", eventPublisher.events.get(0).type());
    }

    @Test
    void liquidatesBreachedIsolatedPositionFromMarketEventAndPublishesSignal() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ));

        MarketOrderExecutionService service = service(
                orderRepository,
                positionRepository,
                accountRepository,
                eventPublisher
        );

        service.onMarketUpdated(new MarketSummaryUpdatedEvent(market(90, 90)));

        assertTrue(positionRepository.findOpenPositions("demo-member").isEmpty());
        assertEquals("POSITION_LIQUIDATED", eventPublisher.events.get(0).type());
        assertEquals(-20.09, eventPublisher.events.get(0).realizedPnl(), 0.0001);
    }

    @Test
    void cancelsCloseOrderWhenOpenPositionQuantityIsSmallerThanOrderQuantity() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.5,
                100,
                100
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "order-close-1",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                1.0,
                105.0,
                false,
                "MAKER",
                0,
                105
        ));

        MarketOrderExecutionService service = service(
                orderRepository,
                positionRepository,
                accountRepository,
                eventPublisher
        );

        service.onMarketUpdated(new MarketSummaryUpdatedEvent(market(106, 106)));

        FuturesOrder cancelled = orderRepository.findByMemberIdAndOrderId("demo-member", "order-close-1").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, cancelled.status());
        assertEquals(0.5, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);
        assertTrue(eventPublisher.events.isEmpty());
    }

    private MarketOrderExecutionService service(
            OrderRepository orderRepository,
            PositionRepository positionRepository,
            AccountRepository accountRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        return new MarketOrderExecutionService(
                orderRepository,
                positionRepository,
                accountRepository,
                new PendingOrderExecutionCache(),
                new LiquidationPolicy(),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        new InMemoryPositionHistoryRepository(),
                        new RewardPointGrantProcessor(new RewardPointPolicy(), new InMemoryRewardPointRepository())
                ),
                eventPublisher
        );
    }

    private MarketSummaryResult market(double lastPrice, double markPrice) {
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

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<TradingExecutionEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof TradingExecutionEvent tradingExecutionEvent) {
                events.add(tradingExecutionEvent);
            }
        }
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, TradingAccount> accounts = new LinkedHashMap<>();

        private InMemoryAccountRepository() {
            accounts.put("demo-member", new TradingAccount(
                    "demo-member",
                    "demo@coinzzickmock.dev",
                    "Demo",
                    100000,
                    100000
            ));
        }

        @Override
        public Optional<TradingAccount> findByMemberId(String memberId) {
            return Optional.ofNullable(accounts.get(memberId));
        }

        @Override
        public TradingAccount save(TradingAccount account) {
            accounts.put(account.memberId(), account);
            return account;
        }
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();

        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            orders.put(key(memberId, futuresOrder.orderId()), new PendingOrderCandidate(memberId, futuresOrder));
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return orders.values().stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(PendingOrderCandidate::order)
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return Optional.ofNullable(orders.get(key(memberId, orderId)))
                    .map(PendingOrderCandidate::order);
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.values().stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .filter(candidate -> candidate.order().isPending())
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
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            if (candidate == null || !candidate.order().isPending()) {
                return Optional.empty();
            }
            FuturesOrder filled = candidate.order().fill(executionPrice, feeType, estimatedFee);
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, filled));
            return Optional.of(filled);
        }

        @Override
        public FuturesOrder updateStatus(String memberId, String orderId, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = status.equals(FuturesOrder.STATUS_CANCELLED)
                    ? candidate.order().cancel()
                    : candidate.order();
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(String memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final Map<String, OpenPositionCandidate> positions = new LinkedHashMap<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return positions.values().stream()
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
            return Optional.ofNullable(positions.get(key(memberId, symbol, positionSide, marginMode)))
                    .map(OpenPositionCandidate::position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.values().stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .toList();
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            positions.put(
                    key(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode()),
                    new OpenPositionCandidate(memberId, positionSnapshot)
            );
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            return positions.remove(key(memberId, symbol, positionSide, marginMode)) != null;
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
        }

        private String key(String memberId, String symbol, String positionSide, String marginMode) {
            return memberId + ":" + symbol + ":" + positionSide + ":" + marginMode;
        }
    }

    private static class InMemoryPositionHistoryRepository implements PositionHistoryRepository {
        private final Map<String, List<PositionHistory>> histories = new LinkedHashMap<>();

        @Override
        public PositionHistory save(String memberId, PositionHistory positionHistory) {
            histories.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(positionHistory);
            return positionHistory;
        }

        @Override
        public List<PositionHistory> findByMemberId(String memberId, String symbol) {
            return histories.getOrDefault(memberId, List.of()).stream()
                    .filter(history -> symbol == null || history.symbol().equals(symbol))
                    .toList();
        }
    }

    private static class InMemoryRewardPointRepository implements RewardPointRepository {
        private RewardPointWallet wallet = new RewardPointWallet("demo-member", 0);

        @Override
        public Optional<RewardPointWallet> findByMemberId(String memberId) {
            return wallet.memberId().equals(memberId) ? Optional.of(wallet) : Optional.empty();
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            this.wallet = rewardPointWallet;
            return rewardPointWallet;
        }
    }
}
