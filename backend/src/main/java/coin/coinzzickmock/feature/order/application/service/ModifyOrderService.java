package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.ModifyOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.ModifyOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
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
    private final OrderPreviewPolicy orderPreviewPolicy;
    private final AccountOrderMutationLock accountOrderMutationLock;

    @Transactional
    public ModifyOrderResult modify(ModifyOrderCommand command) {
        double nextLimitPrice = validatedLimitPrice(command.limitPrice());
        accountOrderMutationLock.lock(command.memberId());

        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(command.memberId(), command.orderId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        validateEditable(order);

        MarketSnapshot market = realtimeMarketPriceReader.requireFreshMarket(order.symbol());
        OrderPlacementRequest request = placementRequest(order, nextLimitPrice);
        OrderPlacementDecision decision = orderPlacementPolicy.decide(request, market.lastPrice());
        if (decision.executable()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        double estimatedFee = estimatedFee(order, request, market.lastPrice());
        FuturesOrder updated = orderRepository.updatePendingLimitPrice(
                command.memberId(),
                order.orderId(),
                nextLimitPrice,
                FEE_TYPE_MAKER,
                estimatedFee,
                nextLimitPrice
        ).orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        rejectIfBecameMarketableAfterUpdate(updated);

        return new ModifyOrderResult(
                updated.orderId(),
                updated.symbol(),
                updated.status(),
                toBigDecimal(updated.limitPrice()),
                updated.feeType(),
                toBigDecimal(updated.estimatedFee()),
                toBigDecimal(updated.executionPrice())
        );
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

    private OrderPlacementRequest placementRequest(FuturesOrder order, double nextLimitPrice) {
        return new OrderPlacementRequest(
                order.orderPurpose(),
                order.positionSide(),
                ORDER_TYPE_LIMIT,
                order.marginMode(),
                nextLimitPrice,
                order.quantity(),
                order.leverage()
        );
    }

    private double estimatedFee(FuturesOrder order, OrderPlacementRequest request, double latestTradePrice) {
        if (order.isClosePositionOrder()) {
            return 0;
        }
        OrderPreview preview = orderPreviewPolicy.preview(request, latestTradePrice);
        return preview.estimatedFee();
    }

    private void rejectIfBecameMarketableAfterUpdate(FuturesOrder updated) {
        MarketSnapshot latestMarket = realtimeMarketPriceReader.requireFreshMarket(updated.symbol());
        OrderPlacementDecision latestDecision = orderPlacementPolicy.decide(
                placementRequest(updated, updated.limitPrice()),
                latestMarket.lastPrice()
        );
        if (!latestDecision.executable()) {
            return;
        }

        throw new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
