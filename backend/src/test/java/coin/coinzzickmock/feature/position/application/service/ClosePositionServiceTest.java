package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClosePositionServiceTest {
    @Test
    void partiallyClosesPositionAndUpdatesAccountAndRewardPoint() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "MARKET", null);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(994.5, result.realizedPnl(), 0.0001);
        assertEquals(0, result.grantedPoint(), 0.0001);

        TradingAccount updatedAccount = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(100994.5, updatedAccount.walletBalance(), 0.0001);
        assertEquals(96994.5, updatedAccount.availableMargin(), 0.0001);

        PositionSnapshot remaining = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, remaining.quantity(), 0.0001);
        assertEquals(1000.0, remaining.unrealizedPnl(), 0.0001);
        assertEquals(90000.0, remaining.liquidationPrice(), 0.0001);

        RewardPointWallet wallet = rewardPointRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(0, wallet.rewardPoint(), 0.0001);
    }

    @Test
    void lossCloseDoesNotGrantPointsAndSettlesAccountWithLoss() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(90000, 90000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "MARKET", null);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(-1004.5, result.realizedPnl(), 0.0001);
        assertEquals(0, result.grantedPoint(), 0.0001);

        TradingAccount updatedAccount = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(98995.5, updatedAccount.walletBalance(), 0.0001);
        assertEquals(94995.5, updatedAccount.availableMargin(), 0.0001);

        PositionSnapshot remaining = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, remaining.quantity(), 0.0001);
        assertEquals(-1000.0, remaining.unrealizedPnl(), 0.0001);

        RewardPointWallet wallet = rewardPointRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(0, wallet.rewardPoint(), 0.0001);
    }

    @Test
    void fullyClosedPositionIsSavedToPositionHistory() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.2, "MARKET", null);

        assertEquals(0.2, result.closedQuantity(), 0.0001);
        assertFalse(positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED").isPresent());

        List<PositionHistory> histories = positionHistoryRepository.findByMemberId("demo-member", null);
        assertEquals(1, histories.size());
        PositionHistory history = histories.get(0);
        assertEquals("BTCUSDT", history.symbol());
        assertEquals("LONG", history.positionSide());
        assertEquals("ISOLATED", history.marginMode());
        assertEquals(10, history.leverage());
        assertEquals(100000, history.averageEntryPrice(), 0.0001);
        assertEquals(110000, history.averageExitPrice(), 0.0001);
        assertEquals(0.2, history.positionSize(), 0.0001);
        assertEquals(1989, history.realizedPnl(), 0.0001);
        assertEquals(2000, history.grossRealizedPnl(), 0.0001);
        assertEquals(0, history.openFee(), 0.0001);
        assertEquals(11, history.closeFee(), 0.0001);
        assertEquals(11, history.totalFee(), 0.0001);
        assertEquals(0, history.fundingCost(), 0.0001);
        assertEquals(1989, history.netRealizedPnl(), 0.0001);
        assertEquals(0.9945, history.roi(), 0.0001);
    }

    @Test
    void fullMarketCloseCancelsExistingPendingCloseOrders() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100000,
                100000
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "existing-close",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.5,
                110000.0,
                false,
                "MAKER",
                0,
                110000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(105000, 105000));

        service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 1, "MARKET", null);

        FuturesOrder cancelled = orderRepository.findByMemberIdAndOrderId("demo-member", "existing-close").orElseThrow();
        assertEquals(FuturesOrder.STATUS_CANCELLED, cancelled.status());
        assertFalse(positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED").isPresent());
    }

    @Test
    void partialMarketCloseReducesPendingCloseOrdersToRemainingPositionQuantity() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100000,
                100000
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "closer",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.3,
                106000.0,
                false,
                "MAKER",
                0,
                106000
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "farther",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.5,
                112000.0,
                false,
                "MAKER",
                0,
                112000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(105000, 105000));

        service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.6, "MARKET", null);

        FuturesOrder closer = orderRepository.findByMemberIdAndOrderId("demo-member", "closer").orElseThrow();
        FuturesOrder reducedFarther = orderRepository.findByMemberIdAndOrderId("demo-member", "farther").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, closer.status());
        assertEquals(0.3, closer.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, reducedFarther.status());
        assertEquals(0.1, reducedFarther.quantity(), 0.0001);
    }

    @Test
    void limitCloseCreatesPendingCloseOrderWithoutClosingPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "LIMIT", 112000.0);

        assertEquals(0, result.closedQuantity(), 0.0001);
        assertEquals(0, positionHistoryRepository.findByMemberId("demo-member", null).size());
        assertEquals(0.2, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);

        FuturesOrder order = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals(FuturesOrder.STATUS_PENDING, order.status());
        assertEquals(FuturesOrder.PURPOSE_CLOSE_POSITION, order.orderPurpose());
        assertEquals("LIMIT", order.orderType());
        assertEquals(112000, order.limitPrice(), 0.0001);
        assertEquals("MAKER", order.feeType());
        assertEquals(0, order.estimatedFee(), 0.0001);
        assertEquals(112000, order.executionPrice(), 0.0001);
    }

    @Test
    void marketableLongLimitCloseFillsImmediatelyAsTakerAndRetainsLimitOrderShape() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "LIMIT", 110000.0);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(994.5, result.realizedPnl(), 0.0001);
        FuturesOrder order = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals("LIMIT", order.orderType());
        assertEquals(110000.0, order.limitPrice(), 0.0001);
        assertEquals(FuturesOrder.PURPOSE_CLOSE_POSITION, order.orderPurpose());
        assertEquals(FuturesOrder.STATUS_FILLED, order.status());
        assertEquals("TAKER", order.feeType());
        assertEquals(110000.0, order.executionPrice(), 0.0001);
        assertEquals(5.5, order.estimatedFee(), 0.0001);
        assertEquals(0.1, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);
        TradingAccount account = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(100994.5, account.walletBalance(), 0.0001);
        assertEquals(96994.5, account.availableMargin(), 0.0001);
        assertEquals(0, positionHistoryRepository.findByMemberId("demo-member", null).size());
    }

    @Test
    void marketableShortLimitCloseFillsImmediatelyAsTakerAtEqualityBoundary() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(90000, 90000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "SHORT", "ISOLATED", 0.1, "LIMIT", 90000.0);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(995.5, result.realizedPnl(), 0.0001);
        FuturesOrder order = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals("LIMIT", order.orderType());
        assertEquals(90000.0, order.limitPrice(), 0.0001);
        assertEquals(FuturesOrder.PURPOSE_CLOSE_POSITION, order.orderPurpose());
        assertEquals(FuturesOrder.STATUS_FILLED, order.status());
        assertEquals("TAKER", order.feeType());
        assertEquals(90000.0, order.executionPrice(), 0.0001);
        assertEquals(4.5, order.estimatedFee(), 0.0001);
        assertEquals(0.1, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "SHORT", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);
        TradingAccount account = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(100995.5, account.walletBalance(), 0.0001);
        assertEquals(96995.5, account.availableMargin(), 0.0001);
    }

    @Test
    void closeRejectsUnsupportedOrderType() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(100000, 100000));

        CoreException thrown = assertThrows(CoreException.class, () -> service.close(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                0.1,
                "STOP",
                101000.0
        ));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void closeRejectsOppositeSidePositionAsNotFound() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(100000, 100000));

        CoreException thrown = assertThrows(CoreException.class, () -> service.close(
                "demo-member",
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                0.1,
                "MARKET",
                null
        ));

        assertEquals(ErrorCode.POSITION_NOT_FOUND, thrown.errorCode());
    }

    @Test
    void longPendingCloseCapReducesHigherLessLikelyLimitPriceFirst() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                70000,
                70000
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "existing-long-close",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.15,
                71000.0,
                false,
                "MAKER",
                0,
                71000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(70000, 70000));

        service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "LIMIT", 72000.0);

        FuturesOrder existing = orderRepository.findByMemberIdAndOrderId("demo-member", "existing-long-close").orElseThrow();
        FuturesOrder reducedNew = orderRepository.findByMemberId("demo-member").stream()
                .filter(order -> order.limitPrice() != null && order.limitPrice() == 72000.0)
                .findFirst()
                .orElseThrow();
        assertEquals(0.15, existing.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, existing.status());
        assertEquals(0.05, reducedNew.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, reducedNew.status());
    }

    @Test
    void shortPendingCloseCapReducesLowerLessLikelyLimitPriceFirst() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                0.2,
                70000,
                70000
        ));
        orderRepository.save("demo-member", FuturesOrder.place(
                "existing-short-close",
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.15,
                69000.0,
                false,
                "MAKER",
                0,
                69000
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(70000, 70000));

        service.close("demo-member", "BTCUSDT", "SHORT", "ISOLATED", 0.1, "LIMIT", 68000.0);

        FuturesOrder existing = orderRepository.findByMemberIdAndOrderId("demo-member", "existing-short-close").orElseThrow();
        FuturesOrder reducedNew = orderRepository.findByMemberId("demo-member").stream()
                .filter(order -> order.limitPrice() != null && order.limitPrice() == 68000.0)
                .findFirst()
                .orElseThrow();
        assertEquals(0.15, existing.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, existing.status());
        assertEquals(0.05, reducedNew.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, reducedNew.status());
    }

    @Test
    void limitCloseSucceedsWhenPendingCloseAlreadyCoversHeldQuantityAndReconcilesToCap() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                70000,
                70000
        ));
        orderRepository.save("demo-member", closeOrder(
                "existing-low",
                "LONG",
                1,
                71000.0,
                java.time.Instant.parse("2026-04-27T00:00:00Z")
        ));
        orderRepository.save("demo-member", closeOrder(
                "existing-high",
                "LONG",
                1,
                73000.0,
                java.time.Instant.parse("2026-04-27T00:01:00Z")
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(70000, 70000));

        service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 1, "LIMIT", 72000.0);

        List<FuturesOrder> pendingCloseOrders = orderRepository.findPendingCloseOrders(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "ISOLATED"
        );
        assertEquals(2, pendingCloseOrders.stream().mapToDouble(FuturesOrder::quantity).sum(), 0.0001);
        assertEquals(FuturesOrder.STATUS_CANCELLED, orderRepository.findByMemberIdAndOrderId("demo-member", "existing-high")
                .orElseThrow()
                .status());
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.findByMemberId("demo-member").stream()
                .filter(order -> order.limitPrice() != null && order.limitPrice() == 72000.0)
                .findFirst()
                .orElseThrow()
                .status());
    }

    @Test
    void pendingCloseCapReducesNewerOrderFirstWhenLikelihoodTies() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                70000,
                70000
        ));
        orderRepository.save("demo-member", closeOrder(
                "older-close",
                "LONG",
                0.8,
                72000.0,
                java.time.Instant.parse("2026-04-27T00:00:00Z")
        ));
        ClosePositionService service = closeService(positionRepository, orderRepository, new FakeProviders(70000, 70000));

        service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.5, "LIMIT", 72000.0);

        FuturesOrder older = orderRepository.findByMemberIdAndOrderId("demo-member", "older-close").orElseThrow();
        FuturesOrder newer = orderRepository.findByMemberId("demo-member").stream()
                .filter(order -> order.limitPrice() != null && order.limitPrice() == 72000.0)
                .filter(order -> !order.orderId().equals("older-close"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.8, older.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, older.status());
        assertEquals(0.2, newer.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, newer.status());
    }

    @Test
    void staleCloseFailsBeforeAccountHistoryOrRewardSideEffects() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        PositionSnapshot stale = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000
        );
        positionRepository.save("demo-member", stale.withVersion(1));
        PositionCloseFinalizer finalizer = new PositionCloseFinalizer(
                positionRepository,
                accountRepository,
                positionHistoryRepository,
                new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                new AfterCommitEventPublisher(event -> {
                })
        );

        CoreException thrown = assertThrows(CoreException.class, () -> finalizer.close(
                "demo-member",
                stale,
                0.1,
                110000,
                110000,
                0.0005,
                PositionHistory.CLOSE_REASON_MANUAL
        ));

        assertEquals(ErrorCode.POSITION_CHANGED, thrown.errorCode());
        TradingAccount account = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(100000, account.walletBalance(), 0.0001);
        assertEquals(95000, account.availableMargin(), 0.0001);
        assertEquals(0, positionHistoryRepository.findByMemberId("demo-member", null).size());
        assertEquals(0, rewardPointRepository.findByMemberId("demo-member").orElseThrow().rewardPoint(), 0.0001);
        assertEquals(0.2, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);
    }

    private static FuturesOrder closeOrderWithTime(String orderId, double quantity, double limitPrice, String orderTime) {
        return new FuturesOrder(
                orderId,
                "BTCUSDT",
                "LONG",
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
                java.time.Instant.parse(orderTime)
        );
    }

    private ClosePositionService closeService(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository,
            Providers providers
    ) {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        return new ClosePositionService(
                positionRepository,
                orderRepository,
                providers,
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        new InMemoryPositionHistoryRepository(),
                        new RewardPointGrantProcessor(new RewardPointPolicy(), new InMemoryRewardPointRepository()),
                        new AfterCommitEventPublisher(event -> {
                        })
                ),
                new PendingCloseOrderCapReconciler(orderRepository),
                new OrderPlacementPolicy()
        );
    }

    private static FuturesOrder closeOrder(
            String orderId,
            String positionSide,
            double quantity,
            Double limitPrice,
            java.time.Instant orderTime
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
                limitPrice == null ? 70000 : limitPrice,
                orderTime
        );
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private TradingAccount account;

        private InMemoryAccountRepository(TradingAccount account) {
            this.account = account;
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
        public List<FuturesOrder> findPendingCloseOrders(String memberId, String symbol, String positionSide, String marginMode) {
            return orders.values().stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(PendingOrderCandidate::order)
                    .filter(FuturesOrder::isPending)
                    .filter(FuturesOrder::isClosePositionOrder)
                    .filter(order -> order.symbol().equals(symbol))
                    .filter(order -> order.positionSide().equals(positionSide))
                    .filter(order -> order.marginMode().equals(marginMode))
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

        @Override
        public FuturesOrder updateQuantityAndStatus(String memberId, String orderId, double quantity, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = candidate.order().withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equals(status)) {
                updated = updated.cancel();
            }
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(String memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final List<PositionSnapshot> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return List.copyOf(positions);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(String memberId, String symbol, String positionSide, String marginMode) {
            return positions.stream()
                    .filter(position -> position.symbol().equals(symbol))
                    .filter(position -> position.positionSide().equals(positionSide))
                    .filter(position -> position.marginMode().equals(marginMode))
                    .findFirst();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.stream()
                    .filter(position -> position.symbol().equals(symbol))
                    .map(position -> new OpenPositionCandidate("demo-member", position))
                    .toList();
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(positionSnapshot);
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            delete(memberId, symbol, positionSide, marginMode);
            return before != positions.size();
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            positions.removeIf(position -> position.symbol().equals(symbol)
                    && position.positionSide().equals(positionSide)
                    && position.marginMode().equals(marginMode));
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

    private static class FakeProviders implements Providers {
        private final double lastPrice;
        private final double markPrice;

        private FakeProviders(double lastPrice, double markPrice) {
            this.lastPrice = lastPrice;
            this.markPrice = markPrice;
        }

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
            return new ConnectorProvider() {
                @Override
                public MarketDataGateway marketDataGateway() {
                    return new MarketDataGateway() {
                        @Override
                        public List<MarketSnapshot> loadSupportedMarkets() {
                            return List.of(loadMarket("BTCUSDT"));
                        }

                        @Override
                        public MarketSnapshot loadMarket(String symbol) {
                            return new MarketSnapshot(symbol, "Bitcoin Perpetual", lastPrice, markPrice, markPrice, 0.0001, 0.1);
                        }
                    };
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
