package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import coin.coinzzickmock.feature.order.application.realtime.PendingOrderExecutionCache;
import coin.coinzzickmock.feature.order.application.realtime.PendingOrderFillProcessor;
import coin.coinzzickmock.feature.order.application.realtime.PositionLiquidationProcessor;
import coin.coinzzickmock.feature.order.application.realtime.PositionTakeProfitStopLossProcessor;
import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.service.AccountOrderMutationLock;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
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
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class MarketOrderExecutionServiceTest {
    private final RealtimeMarketDataStore realtimeMarketDataStore = new RealtimeMarketDataStore();
    private final AtomicInteger tradeSequence = new AtomicInteger();

    @Test
    void fillsExecutablePendingOrderFromMarketEventAndPublishesSignal() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, new FuturesOrder(
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

        service.onMarketUpdated(marketEvent(101, 98, 98));

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(1L, "order-1").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(99, filled.executionPrice(), 0.0001);
        assertTrue(positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isPresent());
        assertEquals("ORDER_FILLED", eventPublisher.events.get(0).type());
    }

    @Test
    void skipsExecutionWhenRealtimeTradeOrTickerIsUnavailable() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, new FuturesOrder(
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

        service.onMarketUpdated(MarketSummaryUpdatedEvent.from(rawMarket(101, 101), rawMarket(98, 98)));

        FuturesOrder pending = orderRepository.findByMemberIdAndOrderId(1L, "order-1").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, pending.status());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void cancelsStalePendingOpenOrderWhenSameSidePositionUsesDifferentMarginMode() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, new FuturesOrder(
                "stale-isolated-open",
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
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                0.1,
                100,
                100
        ));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(101, 98, 98));

        FuturesOrder stale = orderRepository.findByMemberIdAndOrderId(1L, "stale-isolated-open").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, stale.status());
        assertEquals(1, positionRepository.findOpenPositions(1L).size());
        assertTrue(positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "CROSS").isPresent());
        assertFalse(positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isPresent());
        assertTrue(eventPublisher.events.isEmpty());
        assertEquals(100000, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void pendingOpenFillUsesExistingPositionLeverageForMarginAndPositionUpdate() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, new FuturesOrder(
                "stale-leverage-open",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                50,
                1,
                99.0,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                0,
                99
        ));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(101, 98, 98));

        PositionSnapshot position = positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(10, position.leverage());
        assertEquals(2, position.quantity(), 0.0001);
        assertEquals(99990.08515, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void orderFilledEventPublishesOnlyAfterTransactionCommit() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, new FuturesOrder(
                "order-after-commit",
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

        TransactionSynchronizationManager.initSynchronization();
        try {
            service(orderRepository, positionRepository, accountRepository, eventPublisher)
                    .onMarketUpdated(marketEvent(101, 98, 98));

            assertTrue(eventPublisher.events.isEmpty());

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertEquals(1, eventPublisher.events.size());
            assertEquals("ORDER_FILLED", eventPublisher.events.get(0).type());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void liquidatesBreachedIsolatedPositionFromMarketEventAndPublishesSignal() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
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

        assertTrue(positionRepository.findOpenPositions(1L).isEmpty());
        assertEquals("POSITION_LIQUIDATED", eventPublisher.events.get(0).type());
        assertEquals(-20.09, eventPublisher.events.get(0).realizedPnl(), 0.0001);
    }

    @Test
    void rolledBackLiquidationDoesNotPublishEvent() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service(orderRepository, positionRepository, accountRepository, eventPublisher)
                    .onMarketUpdated(new MarketSummaryUpdatedEvent(market(90, 90)));

            assertTrue(eventPublisher.events.isEmpty());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void cancelsCloseOrderWhenOpenPositionQuantityIsSmallerThanOrderQuantity() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.5,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
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

        service.onMarketUpdated(marketEvent(100, 106, 106));

        FuturesOrder cancelled = orderRepository.findByMemberIdAndOrderId(1L, "order-close-1").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, cancelled.status());
        assertEquals(0.5, positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void filledCloseLimitReducesOtherPendingCloseOrdersToRemainingPositionQuantity() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, pendingCloseOrderAt("fill-close", "LONG", 0.6, 105, Instant.parse("2026-04-27T00:00:00Z")));
        orderRepository.save(1L, pendingCloseOrderAt("other-close", "LONG", 0.6, 106, Instant.parse("2026-04-27T00:00:01Z")));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(100, 105, 105));

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(1L, "fill-close").orElseThrow();
        FuturesOrder reduced = orderRepository.findByMemberIdAndOrderId(1L, "other-close").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(FuturesOrder.STATUS_PENDING, reduced.status());
        assertEquals(0.4, reduced.quantity(), 0.0001);
    }

    @Test
    void sameTickCloseLimitUsesReconciledQuantityForLaterCandidate() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, pendingCloseOrderAt("first-close", "LONG", 0.6, 105, Instant.parse("2026-04-27T00:00:00Z")));
        orderRepository.save(1L, pendingCloseOrderAt("second-close", "LONG", 0.6, 106, Instant.parse("2026-04-27T00:00:01Z")));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(100, 106, 106));

        FuturesOrder first = orderRepository.findByMemberIdAndOrderId(1L, "first-close").orElseThrow();
        FuturesOrder second = orderRepository.findByMemberIdAndOrderId(1L, "second-close").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, first.status());
        assertEquals(0.6, first.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_FILLED, second.status());
        assertEquals(0.4, second.quantity(), 0.0001);
        assertTrue(positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
    }

    @Test
    void liquidationCancelsSamePositionPendingCloseOrders() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ));
        orderRepository.save(1L, pendingCloseOrderAt("close-before-liquidation", "LONG", 0.5, 105, Instant.parse("2026-04-27T00:00:00Z")));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(new MarketSummaryUpdatedEvent(market(90, 90)));

        FuturesOrder cancelled = orderRepository.findByMemberIdAndOrderId(1L, "close-before-liquidation").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, cancelled.status());
    }

    @Test
    void takeProfitTriggerClosesPositionAndCancelsPendingCloseOrders() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));
        orderRepository.save(1L, pendingCloseOrderAt(
                "close-after-tp",
                "LONG",
                0.5,
                110,
                Instant.parse("2026-04-27T00:00:00Z")
        ));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(new MarketSummaryUpdatedEvent(market(106, 106)));

        assertTrue(positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
        assertEquals("POSITION_TAKE_PROFIT", eventPublisher.events.get(0).type());
        FuturesOrder cancelled = orderRepository.findByMemberIdAndOrderId(1L, "close-after-tp").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, cancelled.status());
    }

    @Test
    void triggeredTakeProfitCancelsOcoStopLossSibling() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "sl-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                "oco-1"
        ));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(new MarketSummaryUpdatedEvent(market(106, 106)));

        FuturesOrder takeProfit = orderRepository.findByMemberIdAndOrderId(1L, "tp-order").orElseThrow();
        FuturesOrder stopLoss = orderRepository.findByMemberIdAndOrderId(1L, "sl-order").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, takeProfit.status());
        assertEquals(FuturesOrder.STATUS_CANCELLED, stopLoss.status());
        assertEquals("POSITION_TAKE_PROFIT", eventPublisher.events.get(0).type());
    }

    @Test
    void stopLossTriggerPublishesOnlyAfterTransactionCommit() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "sl-order",
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                104,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                null
        ));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service(orderRepository, positionRepository, accountRepository, eventPublisher)
                    .onMarketUpdated(new MarketSummaryUpdatedEvent(market(105, 105)));

            assertTrue(eventPublisher.events.isEmpty());

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertEquals(1, eventPublisher.events.size());
            assertEquals("POSITION_STOP_LOSS", eventPublisher.events.get(0).type());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void nonBreachedLiquidationAssessmentDoesNotPersistMarkOnlyPositionChanges() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        PositionSnapshot original = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ).withVersion(7);
        positionRepository.save(1L, original);
        int savesBeforeMarketEvent = positionRepository.saveCount;

        MarketOrderExecutionService service = service(
                orderRepository,
                positionRepository,
                accountRepository,
                eventPublisher
        );

        service.onMarketUpdated(new MarketSummaryUpdatedEvent(market(105, 105)));

        assertEquals(savesBeforeMarketEvent, positionRepository.saveCount);
        PositionSnapshot persisted = positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(100, persisted.markPrice(), 0.0001);
        assertEquals(0, persisted.unrealizedPnl(), 0.0001);
        assertEquals(7, persisted.version());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void risingPriceFillsSellSideLimitsByPathPriceBeforeCreationTime() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, pendingOpenOrderAt("later-price", "SHORT", 103, Instant.parse("2026-04-27T00:00:00Z")));
        orderRepository.save(1L, pendingOpenOrderAt("same-price-second", "SHORT", 102, Instant.parse("2026-04-27T00:00:02Z")));
        orderRepository.save(1L, pendingOpenOrderAt("same-price-first", "SHORT", 102, Instant.parse("2026-04-27T00:00:01Z")));
        orderRepository.save(1L, pendingOpenOrderAt("buy-side-ignored", "LONG", 101, Instant.parse("2026-04-27T00:00:03Z")));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(100, 104, 104));

        assertEquals(3, eventPublisher.events.size());
        assertEquals("same-price-first", eventPublisher.events.get(0).orderId());
        assertEquals(102, eventPublisher.events.get(0).executionPrice(), 0.0001);
        assertEquals("same-price-second", eventPublisher.events.get(1).orderId());
        assertEquals(102, eventPublisher.events.get(1).executionPrice(), 0.0001);
        assertEquals("later-price", eventPublisher.events.get(2).orderId());
        assertEquals(103, eventPublisher.events.get(2).executionPrice(), 0.0001);
        PositionSnapshot opened = positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "ISOLATED")
                .orElseThrow();
        assertEquals(0.004605, opened.accumulatedOpenFee(), 0.000001);
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.findByMemberIdAndOrderId(1L, "buy-side-ignored")
                .orElseThrow()
                .status());
    }

    @Test
    void fallingPriceFillsBuySideLimitsFromHigherPriceToLowerPrice() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        orderRepository.save(1L, pendingOpenOrderAt("lower-price", "LONG", 97, Instant.parse("2026-04-27T00:00:00Z")));
        orderRepository.save(1L, pendingOpenOrderAt("higher-price", "LONG", 99, Instant.parse("2026-04-27T00:00:01Z")));
        orderRepository.save(1L, pendingOpenOrderAt("sell-side-ignored", "SHORT", 98, Instant.parse("2026-04-27T00:00:02Z")));

        service(orderRepository, positionRepository, accountRepository, eventPublisher)
                .onMarketUpdated(marketEvent(100, 96, 96));

        assertEquals(2, eventPublisher.events.size());
        assertEquals("higher-price", eventPublisher.events.get(0).orderId());
        assertEquals(99, eventPublisher.events.get(0).executionPrice(), 0.0001);
        assertEquals("lower-price", eventPublisher.events.get(1).orderId());
        assertEquals(97, eventPublisher.events.get(1).executionPrice(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.findByMemberIdAndOrderId(1L, "sell-side-ignored")
                .orElseThrow()
                .status());
    }

    private MarketOrderExecutionService service(
            OrderRepository orderRepository,
            PositionRepository positionRepository,
            AccountRepository accountRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        AfterCommitEventPublisher afterCommitEventPublisher = new AfterCommitEventPublisher(eventPublisher);
        PositionCloseFinalizer positionCloseFinalizer = new PositionCloseFinalizer(
                positionRepository,
                accountRepository,
                new InMemoryPositionHistoryRepository(),
                new RewardPointGrantProcessor(new RewardPointPolicy(), new InMemoryRewardPointRepository()),
                afterCommitEventPublisher
        );
        PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler = new PendingCloseOrderCapReconciler(orderRepository);
        RealtimeMarketPriceReader realtimeMarketPriceReader = new RealtimeMarketPriceReader(realtimeMarketDataStore);
        AccountOrderMutationLock accountOrderMutationLock = new AccountOrderMutationLock(accountRepository);
        return new MarketOrderExecutionService(
                new PendingOrderFillProcessor(
                        orderRepository,
                        positionRepository,
                        new PendingOrderExecutionCache(),
                        positionCloseFinalizer,
                        pendingCloseOrderCapReconciler,
                        afterCommitEventPublisher,
                        realtimeMarketPriceReader,
                        new FilledOpenOrderApplier(accountRepository, positionRepository, afterCommitEventPublisher),
                        accountOrderMutationLock
                ),
                new PositionLiquidationProcessor(
                        positionRepository,
                        accountRepository,
                        new LiquidationPolicy(),
                        positionCloseFinalizer,
                        pendingCloseOrderCapReconciler,
                        afterCommitEventPublisher,
                        realtimeMarketPriceReader,
                        accountOrderMutationLock
                ),
                new PositionTakeProfitStopLossProcessor(
                        orderRepository,
                        positionRepository,
                        positionCloseFinalizer,
                        pendingCloseOrderCapReconciler,
                        afterCommitEventPublisher,
                        realtimeMarketPriceReader,
                        accountOrderMutationLock
                )
        );
    }

    private MarketSummaryResult market(double lastPrice, double markPrice) {
        seedRealtimeMarket(lastPrice, markPrice);
        return rawMarket(lastPrice, markPrice);
    }

    private MarketSummaryResult rawMarket(double lastPrice, double markPrice) {
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

    private MarketSummaryUpdatedEvent marketEvent(double previousLastPrice, double lastPrice, double markPrice) {
        return MarketSummaryUpdatedEvent.from(market(previousLastPrice, previousLastPrice), market(lastPrice, markPrice));
    }

    private FuturesOrder pendingOpenOrderAt(String orderId, String positionSide, double limitPrice, Instant orderTime) {
        return new FuturesOrder(
                orderId,
                "BTCUSDT",
                positionSide,
                "LIMIT",
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

    private FuturesOrder pendingCloseOrderAt(
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
                "LIMIT",
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
        private final Map<Long, TradingAccount> accounts = new LinkedHashMap<>();

        private InMemoryAccountRepository() {
            accounts.put(1L, new TradingAccount(
                    1L,
                    "demo@coinzzickmock.dev",
                    "Demo",
                    100000,
                    100000
            ));
        }

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
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
        public AccountMutationResult updateWithVersion(
                TradingAccount expectedAccount,
                TradingAccount nextAccount
        ) {
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

    private static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();

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
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .filter(candidate -> candidate.order().isPending())
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
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = status.equals(FuturesOrder.STATUS_CANCELLED)
                    ? candidate.order().cancel()
                    : candidate.order();
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = candidate.order().withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equals(status)) {
                updated = updated.cancel();
            }
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(Long memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final Map<String, OpenPositionCandidate> positions = new LinkedHashMap<>();
        private int saveCount;

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
            return positions.values().stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .toList();
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            saveCount++;
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

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
        }

        private String key(Long memberId, String symbol, String positionSide, String marginMode) {
            return memberId + ":" + symbol + ":" + positionSide + ":" + marginMode;
        }
    }

    private static class InMemoryPositionHistoryRepository implements PositionHistoryRepository {
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

    private static class InMemoryRewardPointRepository implements RewardPointRepository {
        private RewardPointWallet wallet = new RewardPointWallet(1L, 0);

        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return wallet.memberId().equals(memberId) ? Optional.of(wallet) : Optional.empty();
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            this.wallet = rewardPointWallet;
            return rewardPointWallet;
        }
    }
}
