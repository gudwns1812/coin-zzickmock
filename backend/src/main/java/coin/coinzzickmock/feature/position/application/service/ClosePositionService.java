package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClosePositionService {
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String ORDER_TYPE_MARKET = "MARKET";
    private static final String FEE_TYPE_MAKER = "MAKER";

    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final OrderPlacementPolicy orderPlacementPolicy;

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
        OrderPlacementDecision decision = orderPlacementPolicy.decide(
                new OrderPlacementRequest(
                        FuturesOrder.PURPOSE_CLOSE_POSITION,
                        positionSide,
                        orderType,
                        limitPrice,
                        quantity,
                        position.leverage()
                ),
                market.lastPrice()
        );

        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType) && !decision.executable()) {
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
            pendingCloseOrderCapReconciler.reconcile(memberId, pendingCloseOrder, position.quantity(), market.lastPrice());
            return new ClosePositionResult(symbol, 0, 0, 0);
        }

        double closeFee = decision.estimatedFee(Math.min(quantity, position.quantity()));
        String closeOrderId = UUID.randomUUID().toString();
        String savedOrderType = ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType) ? ORDER_TYPE_LIMIT : ORDER_TYPE_MARKET;
        orderRepository.save(memberId, FuturesOrder.place(
                closeOrderId,
                symbol,
                positionSide,
                savedOrderType,
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                marginMode,
                position.leverage(),
                quantity,
                ORDER_TYPE_LIMIT.equals(savedOrderType) ? limitPrice : null,
                true,
                decision.feeType(),
                closeFee,
                decision.executionPrice()
        ));

        ClosePositionResult result = positionCloseFinalizer.close(
                memberId,
                position,
                quantity,
                market.markPrice(),
                decision.executionPrice(),
                decision.feeRate(),
                PositionHistory.CLOSE_REASON_MANUAL,
                WalletHistorySource.positionCloseOrderFill(closeOrderId)
        );
        pendingCloseOrderCapReconciler.reconcile(
                memberId,
                position,
                Math.max(0, position.quantity() - quantity),
                market.lastPrice()
        );
        return result;
    }

    private MarketSnapshot loadMarket(String symbol) {
        return realtimeMarketPriceReader.requireFreshMarket(symbol);
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

        if (!ORDER_TYPE_MARKET.equalsIgnoreCase(orderType) && !ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "주문 유형을 확인해주세요.");
        }

        if (ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType)
                && (limitPrice == null || !Double.isFinite(limitPrice) || limitPrice <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "지정가 종료 가격을 확인해주세요.");
        }
    }

}
