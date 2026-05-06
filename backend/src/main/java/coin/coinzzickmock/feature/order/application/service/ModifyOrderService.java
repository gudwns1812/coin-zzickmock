package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.ModifyOrderCommand;
import coin.coinzzickmock.feature.order.application.fill.MarketableEditedOrderFiller;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.ModifyOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModifyOrderService {
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String FEE_TYPE_MAKER = "MAKER";

    private final OrderRepository orderRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final AccountOrderMutationLock accountOrderMutationLock;
    private final MarketableEditedOrderFiller marketableEditedOrderFiller;

    @Transactional
    public ModifyOrderResult modify(ModifyOrderCommand command) {
        double nextLimitPrice = validatedLimitPrice(command.limitPrice());
        accountOrderMutationLock.lock(command.memberId());

        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(command.memberId(), command.orderId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        validateEditable(order);

        double estimatedFee = pendingMakerEstimatedFee(order, nextLimitPrice);
        FuturesOrder updated = orderRepository.updatePendingLimitPrice(
                command.memberId(),
                order.orderId(),
                nextLimitPrice,
                FEE_TYPE_MAKER,
                estimatedFee,
                nextLimitPrice
        ).orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        FuturesOrder result = fillIfMarketable(command.memberId(), updated);

        return result(result);
    }

    private BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private double validatedLimitPrice(BigDecimal limitPrice) {
        if (limitPrice == null || limitPrice.signum() <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        double limitPriceValue = limitPrice.doubleValue();
        if (!Double.isFinite(limitPriceValue) || limitPriceValue <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return limitPriceValue;
    }

    private void validateEditable(FuturesOrder order) {
        if (!order.isPending()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (!ORDER_TYPE_LIMIT.equalsIgnoreCase(order.orderType()) || order.limitPrice() == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (order.isConditionalOrder()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private OrderPlacementRequest placementRequest(FuturesOrder order, double limitPrice) {
        return new OrderPlacementRequest(
                order.orderPurpose(),
                order.positionSide(),
                ORDER_TYPE_LIMIT,
                order.marginMode(),
                limitPrice,
                order.quantity(),
                order.leverage()
        );
    }

    private double pendingMakerEstimatedFee(FuturesOrder order, double nextLimitPrice) {
        if (order.isClosePositionOrder()) {
            return 0;
        }
        return nextLimitPrice * order.quantity() * OrderPlacementPolicy.MAKER_FEE_RATE;
    }

    private FuturesOrder fillIfMarketable(Long memberId, FuturesOrder updated) {
        MarketSnapshot latestMarket = realtimeMarketPriceReader.requireFreshMarket(updated.symbol());
        var latestDecision = orderPlacementPolicy.decide(
                placementRequest(updated, updated.limitPrice()),
                latestMarket.lastPrice()
        );
        if (!latestDecision.executable()) {
            return updated;
        }

        return marketableEditedOrderFiller.fill(memberId, updated, latestMarket, latestDecision);
    }

    private ModifyOrderResult result(FuturesOrder order) {
        return new ModifyOrderResult(
                order.orderId(),
                order.symbol(),
                order.status(),
                toBigDecimal(order.limitPrice()),
                order.feeType(),
                toBigDecimal(order.estimatedFee()),
                toBigDecimal(order.executionPrice())
        );
    }
}
