package coin.coinzzickmock.feature.order.domain;

import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderPreviewPolicy {
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String ORDER_TYPE_MARKET = "MARKET";
    private static final String POSITION_SIDE_LONG = "LONG";
    private static final String FEE_TYPE_TAKER = "TAKER";
    private static final String FEE_TYPE_MAKER = "MAKER";
    private static final BigDecimal TAKER_FEE_RATE = BigDecimal.valueOf(0.0005);
    private static final BigDecimal MAKER_FEE_RATE = BigDecimal.valueOf(0.00015);
    private static final int DIVISION_SCALE = 8;

    public OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        BigDecimal entryPrice = resolveEntryPrice(command, marketSnapshot);
        boolean executable = isExecutable(command, marketSnapshot.lastPrice(), entryPrice.doubleValue());
        BigDecimal feeRate = executable ? TAKER_FEE_RATE : MAKER_FEE_RATE;
        BigDecimal quantity = decimal(command.quantity());
        BigDecimal estimatedFee = entryPrice.multiply(quantity).multiply(feeRate);
        BigDecimal estimatedInitialMargin = entryPrice.multiply(quantity)
                .divide(BigDecimal.valueOf(command.leverage()), DIVISION_SCALE, RoundingMode.HALF_UP);
        BigDecimal liquidationGap = entryPrice.divide(
                BigDecimal.valueOf(command.leverage()),
                DIVISION_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal estimatedLiquidationPrice = isLong(command.positionSide())
                ? entryPrice.subtract(liquidationGap)
                : entryPrice.add(liquidationGap);

        return new OrderPreview(
                executable ? FEE_TYPE_TAKER : FEE_TYPE_MAKER,
                estimatedFee.doubleValue(),
                estimatedInitialMargin.doubleValue(),
                estimatedLiquidationPrice.doubleValue(),
                entryPrice.doubleValue(),
                executable
        );
    }

    private BigDecimal resolveEntryPrice(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(command.orderType()) && command.limitPrice() != null) {
            return decimal(command.limitPrice());
        }
        return decimal(marketSnapshot.lastPrice());
    }

    private boolean isExecutable(CreateOrderCommand command, double lastPrice, double entryPrice) {
        if (ORDER_TYPE_MARKET.equalsIgnoreCase(command.orderType())) {
            return true;
        }
        if (isLong(command.positionSide())) {
            return lastPrice <= entryPrice;
        }
        return lastPrice >= entryPrice;
    }

    private boolean isLong(String positionSide) {
        return POSITION_SIDE_LONG.equalsIgnoreCase(positionSide);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
