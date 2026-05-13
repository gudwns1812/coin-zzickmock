package coin.coinzzickmock.feature.order.domain;

public class OrderPlacementPolicy {
    public static final String FEE_TYPE_TAKER = "TAKER";
    public static final String FEE_TYPE_MAKER = "MAKER";
    public static final double TAKER_FEE_RATE = 0.0005d;
    public static final double MAKER_FEE_RATE = 0.00015d;

    private static final String ORDER_TYPE_MARKET = "MARKET";
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String POSITION_SIDE_LONG = "LONG";

    public OrderPlacementDecision decide(OrderPlacementRequest request, double latestTradePrice) {
        if (ORDER_TYPE_MARKET.equalsIgnoreCase(request.orderType())) {
            return taker(latestTradePrice);
        }

        if (request.limitPrice() == null) {
            return taker(latestTradePrice);
        }

        double limitPrice = request.limitPrice();
        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(request.orderType()) && isMarketableLimit(request, latestTradePrice)) {
            return taker(latestTradePrice);
        }

        return new OrderPlacementDecision(
                false,
                FEE_TYPE_MAKER,
                MAKER_FEE_RATE,
                limitPrice,
                limitPrice
        );
    }

    public boolean isBuySide(OrderPlacementRequest request) {
        return (FuturesOrder.PURPOSE_OPEN_POSITION.equalsIgnoreCase(request.orderPurpose()) && isLong(request.positionSide()))
                || (FuturesOrder.PURPOSE_CLOSE_POSITION.equalsIgnoreCase(request.orderPurpose()) && !isLong(request.positionSide()));
    }

    private boolean isMarketableLimit(OrderPlacementRequest request, double latestTradePrice) {
        if (request.limitPrice() == null) {
            return false;
        }
        if (isBuySide(request)) {
            return latestTradePrice <= request.limitPrice();
        }
        return latestTradePrice >= request.limitPrice();
    }

    private OrderPlacementDecision taker(double latestTradePrice) {
        return new OrderPlacementDecision(
                true,
                FEE_TYPE_TAKER,
                TAKER_FEE_RATE,
                latestTradePrice,
                latestTradePrice
        );
    }

    private boolean isLong(String positionSide) {
        return POSITION_SIDE_LONG.equalsIgnoreCase(positionSide);
    }
}
