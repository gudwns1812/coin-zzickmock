package coin.coinzzickmock.feature.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPlacementPolicyTest {
    private final OrderPlacementPolicy policy = new OrderPlacementPolicy();

    @Test
    void marketOrdersAreTakerAtLatestTradePrice() {
        OrderPlacementDecision decision = policy.decide(request(
                FuturesOrder.PURPOSE_OPEN_POSITION,
                "LONG",
                "MARKET",
                null
        ), 74700);

        assertTrue(decision.executable());
        assertEquals("TAKER", decision.feeType());
        assertEquals(0.0005, decision.feeRate(), 0.0000001);
        assertEquals(74700, decision.executionPrice(), 0.0001);
        assertEquals(74700, decision.estimatePrice(), 0.0001);
    }

    @Test
    void buySideLimitsAreMarketableAtOrBelowLimitPrice() {
        assertMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "LONG", 75000, 75000);
        assertMarketable(FuturesOrder.PURPOSE_CLOSE_POSITION, "SHORT", 75000, 75000);
        assertMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "LONG", 74700, 75000);
        assertNotMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "LONG", 75300, 75000);
    }

    @Test
    void sellSideLimitsAreMarketableAtOrAboveLimitPrice() {
        assertMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "SHORT", 75000, 75000);
        assertMarketable(FuturesOrder.PURPOSE_CLOSE_POSITION, "LONG", 75000, 75000);
        assertMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "SHORT", 75300, 75000);
        assertNotMarketable(FuturesOrder.PURPOSE_OPEN_POSITION, "SHORT", 74700, 75000);
    }

    @Test
    void nonMarketableLimitsRemainMakerAtLimitPrice() {
        OrderPlacementDecision decision = policy.decide(request(
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "LONG",
                "LIMIT",
                75200.0
        ), 74700);

        assertFalse(decision.executable());
        assertEquals("MAKER", decision.feeType());
        assertEquals(0.00015, decision.feeRate(), 0.0000001);
        assertEquals(75200, decision.executionPrice(), 0.0001);
        assertEquals(75200, decision.estimatePrice(), 0.0001);
    }

    @Test
    void nullLimitPriceFallsBackToLatestTradePriceLikeExistingPreviewBehavior() {
        OrderPlacementDecision decision = policy.decide(request(
                FuturesOrder.PURPOSE_OPEN_POSITION,
                "LONG",
                "LIMIT",
                null
        ), 74700);

        assertTrue(decision.executable());
        assertEquals("TAKER", decision.feeType());
        assertEquals(74700, decision.executionPrice(), 0.0001);
        assertEquals(74700, decision.estimatePrice(), 0.0001);
    }

    private void assertMarketable(String purpose, String side, double latestTradePrice, double limitPrice) {
        OrderPlacementDecision decision = policy.decide(request(purpose, side, "LIMIT", limitPrice), latestTradePrice);

        assertTrue(decision.executable());
        assertEquals("TAKER", decision.feeType());
        assertEquals(latestTradePrice, decision.executionPrice(), 0.0001);
        assertEquals(latestTradePrice, decision.estimatePrice(), 0.0001);
    }

    private void assertNotMarketable(String purpose, String side, double latestTradePrice, double limitPrice) {
        OrderPlacementDecision decision = policy.decide(request(purpose, side, "LIMIT", limitPrice), latestTradePrice);

        assertFalse(decision.executable());
        assertEquals("MAKER", decision.feeType());
        assertEquals(limitPrice, decision.executionPrice(), 0.0001);
        assertEquals(limitPrice, decision.estimatePrice(), 0.0001);
    }

    private OrderPlacementRequest request(String purpose, String positionSide, String orderType, Double limitPrice) {
        return new OrderPlacementRequest(purpose, positionSide, orderType, "ISOLATED", limitPrice, 0.1, 10);
    }
}
