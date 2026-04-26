package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.providers.Providers;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClosePositionService {
    private static final double TAKER_FEE_RATE = 0.0005;
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String ORDER_TYPE_MARKET = "MARKET";
    private static final String FEE_TYPE_MAKER = "MAKER";
    private static final String FEE_TYPE_TAKER = "TAKER";

    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final Providers providers;
    private final PositionCloseFinalizer positionCloseFinalizer;

    @Transactional
    public ClosePositionResult close(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double quantity,
            String orderType,
            Double limitPrice
    ) {
        PositionSnapshot position = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));
        validateCloseRequest(quantity, orderType, limitPrice, position);

        MarketSnapshot market = loadMarket(symbol);
        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType)) {
            FuturesOrder pendingCloseOrder = orderRepository.save(memberId, FuturesOrder.place(
                    UUID.randomUUID().toString(),
                    symbol,
                    positionSide,
                    ORDER_TYPE_LIMIT,
                    FuturesOrder.PURPOSE_CLOSE_POSITION,
                    marginMode,
                    position.leverage(),
                    quantity,
                    limitPrice,
                    false,
                    FEE_TYPE_MAKER,
                    0,
                    limitPrice == null ? market.lastPrice() : limitPrice
            ));
            reconcilePendingCloseOrderCap(memberId, pendingCloseOrder, position.quantity(), market.lastPrice());
            return new ClosePositionResult(symbol, 0, 0, 0);
        }

        double closeFee = market.lastPrice() * Math.min(quantity, position.quantity()) * TAKER_FEE_RATE;
        orderRepository.save(memberId, FuturesOrder.place(
                UUID.randomUUID().toString(),
                symbol,
                positionSide,
                ORDER_TYPE_MARKET,
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                marginMode,
                position.leverage(),
                quantity,
                null,
                true,
                FEE_TYPE_TAKER,
                closeFee,
                market.lastPrice()
        ));

        return positionCloseFinalizer.close(
                memberId,
                position,
                quantity,
                market.markPrice(),
                market.lastPrice(),
                TAKER_FEE_RATE,
                PositionHistory.CLOSE_REASON_MANUAL
        );
    }

    private MarketSnapshot loadMarket(String symbol) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(symbol);
        if (snapshot == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }
        return snapshot;
    }

    private void validateCloseRequest(
            double quantity,
            String orderType,
            Double limitPrice,
            PositionSnapshot position
    ) {
        if (!Double.isFinite(quantity) || quantity <= 0 || quantity > position.quantity()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "종료 수량을 확인해주세요.");
        }

        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType)
                && (limitPrice == null || !Double.isFinite(limitPrice) || limitPrice <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "지정가 종료 가격을 확인해주세요.");
        }
    }

    private void reconcilePendingCloseOrderCap(
            String memberId,
            FuturesOrder submittedOrder,
            double heldQuantity,
            double currentPrice
    ) {
        List<FuturesOrder> pendingCloseOrders = orderRepository.findPendingCloseOrders(
                memberId,
                submittedOrder.symbol(),
                submittedOrder.positionSide(),
                submittedOrder.marginMode()
        );

        double pendingQuantity = pendingCloseOrders.stream()
                .mapToDouble(FuturesOrder::quantity)
                .sum();
        double excessQuantity = pendingQuantity - heldQuantity;
        if (excessQuantity <= 0) {
            return;
        }

        for (FuturesOrder order : pendingCloseOrders.stream()
                .sorted(leastLikelyToExecuteFirst(submittedOrder.positionSide(), currentPrice))
                .toList()) {
            if (excessQuantity <= 0) {
                return;
            }
            double reduction = Math.min(order.quantity(), excessQuantity);
            double nextQuantity = order.quantity() - reduction;
            if (nextQuantity <= 0) {
                orderRepository.updateStatus(memberId, order.orderId(), FuturesOrder.STATUS_CANCELLED);
            } else {
                orderRepository.updateQuantityAndStatus(memberId, order.orderId(), nextQuantity, FuturesOrder.STATUS_PENDING);
            }
            excessQuantity -= reduction;
        }
    }

    private Comparator<FuturesOrder> leastLikelyToExecuteFirst(String positionSide, double currentPrice) {
        Comparator<FuturesOrder> priceComparator;
        if ("LONG".equalsIgnoreCase(positionSide)) {
            priceComparator = Comparator.comparing(
                    (FuturesOrder order) -> order.limitPrice() == null ? currentPrice : order.limitPrice()
            ).reversed();
        } else {
            priceComparator = Comparator.comparing(
                    order -> order.limitPrice() == null ? currentPrice : order.limitPrice()
            );
        }
        return priceComparator.thenComparing(Comparator.comparing(FuturesOrder::orderTime).reversed());
    }
}
