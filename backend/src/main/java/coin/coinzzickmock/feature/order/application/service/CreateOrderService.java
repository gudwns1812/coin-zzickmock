package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.service.FilledOpenOrderApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CreateOrderService {
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String ORDER_TYPE_MARKET = "MARKET";

    private final OrderPreviewPolicy orderPreviewPolicy;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final FilledOpenOrderApplier filledOpenOrderApplier;

    @Transactional(readOnly = true)
    public OrderPreview preview(CreateOrderCommand command) {
        validateOrderType(command.orderType());
        validateExistingPositionInvariants(command);
        return preview(command, loadMarket(command.symbol()));
    }

    @Transactional
    public CreateOrderResult execute(CreateOrderCommand command) {
        validateOrderType(command.orderType());
        validateExistingPositionInvariants(command);
        MarketSnapshot marketSnapshot = loadMarket(command.symbol());
        OrderPlacementRequest placementRequest = placementRequest(command);
        OrderPlacementDecision decision = orderPlacementPolicy.decide(placementRequest, marketSnapshot.lastPrice());
        OrderPreview preview = orderPreviewPolicy.preview(placementRequest, marketSnapshot.lastPrice());
        String orderId = UUID.randomUUID().toString();

        FuturesOrder futuresOrder = orderRepository.save(
                command.memberId(),
                FuturesOrder.place(
                        orderId,
                        command.symbol(),
                        command.positionSide(),
                        command.orderType(),
                        FuturesOrder.PURPOSE_OPEN_POSITION,
                        command.marginMode(),
                        command.leverage(),
                        command.quantity(),
                        command.limitPrice(),
                        decision.executable(),
                        decision.feeType(),
                        preview.estimatedFee(),
                        decision.executionPrice()
                )
        );

        if (decision.executable()) {
            applyFilledOrder(command, orderId, marketSnapshot, preview, decision.executionPrice());
        }

        return new CreateOrderResult(
                futuresOrder.orderId(),
                futuresOrder.status(),
                futuresOrder.symbol(),
                futuresOrder.feeType(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                preview.estimatedLiquidationPrice(),
                futuresOrder.executionPrice()
        );
    }

    private void applyFilledOrder(
            CreateOrderCommand command,
            String orderId,
            MarketSnapshot marketSnapshot,
            OrderPreview preview,
            double executionPrice
    ) {
        filledOpenOrderApplier.apply(new FilledOpenOrder(
                command.memberId(),
                orderId,
                command.symbol(),
                command.positionSide(),
                command.marginMode(),
                command.leverage(),
                command.quantity(),
                executionPrice,
                marketSnapshot.markPrice(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                null
        ));
    }

    private OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        return orderPreviewPolicy.preview(placementRequest(command), marketSnapshot.lastPrice());
    }

    private void validateExistingPositionInvariants(CreateOrderCommand command) {
        Optional<PositionSnapshot> existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide()
        );
        if (existing.isEmpty()) {
            return;
        }

        PositionSnapshot position = existing.orElseThrow();
        if (!position.marginMode().equalsIgnoreCase(command.marginMode())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "기존 포지션과 다른 마진 모드로 주문할 수 없습니다.");
        }
        if (position.leverage() != command.leverage()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "기존 포지션 레버리지를 먼저 적용한 뒤 주문해주세요.");
        }
    }

    private OrderPlacementRequest placementRequest(CreateOrderCommand command) {
        return new OrderPlacementRequest(
                FuturesOrder.PURPOSE_OPEN_POSITION,
                command.positionSide(),
                command.orderType(),
                command.limitPrice(),
                command.quantity(),
                command.leverage()
        );
    }

    private MarketSnapshot loadMarket(String symbol) {
        return realtimeMarketPriceReader.requireFreshMarket(symbol);
    }

    private void validateOrderType(String orderType) {
        if (!ORDER_TYPE_MARKET.equalsIgnoreCase(orderType) && !ORDER_TYPE_LIMIT.equalsIgnoreCase(orderType)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "주문 유형을 확인해주세요.");
        }
    }
}
