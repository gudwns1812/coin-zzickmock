package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.dto.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.implement.OrderCrossMarginPreviewProjector;
import coin.coinzzickmock.feature.order.application.implement.OrderFillApplier;
import coin.coinzzickmock.feature.order.application.implement.OrderFillApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.application.implement.OrderMutationLock;
import coin.coinzzickmock.feature.order.application.implement.OrderPlacementFactory;
import coin.coinzzickmock.feature.order.application.implement.OrderPositionInvariantValidator;
import coin.coinzzickmock.feature.order.application.implement.OrderPostSaveFillHandler;
import coin.coinzzickmock.feature.order.application.implement.OrderPostSaveFillHandler.PostSaveLimitFill;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingLimitOrderBook;
import coin.coinzzickmock.feature.order.application.dto.CreateOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CreateOrderService {
    private final OrderPreviewPolicy orderPreviewPolicy;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderRepository orderRepository;
    private final OrderPlacementFactory orderPlacementFactory;
    private final OrderPositionInvariantValidator orderPositionInvariantValidator;
    private final OrderCrossMarginPreviewProjector orderCrossMarginPreviewProjector;
    private final OrderFillApplier orderFillApplier;
    private final OrderPostSaveFillHandler orderPostSaveFillHandler;
    private final OrderMutationLock orderMutationLock;
    private final OrderPendingLimitOrderBook pendingLimitOrderBook;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional(readOnly = true)
    public OrderPreview preview(CreateOrderCommand command) {
        validateOrderType(command.orderType());
        orderPositionInvariantValidator.validateOpenPositionCompatibility(command);
        return preview(command, loadMarket(command.symbol()));
    }

    @Transactional
    public CreateOrderResult execute(CreateOrderCommand command) {
        orderMutationLock.lock(command.memberId());
        validateOrderType(command.orderType());
        orderPositionInvariantValidator.validateOpenPositionCompatibility(command);

        MarketSnapshot marketSnapshot = loadMarket(command.symbol());
        OrderPlacementRequest placementRequest = orderPlacementFactory.openPosition(command);
        OrderPlacementDecision decision = orderPlacementPolicy.decide(placementRequest, marketSnapshot.lastPrice());
        OrderPreview preview = preview(command, marketSnapshot);
        String orderId = UUID.randomUUID().toString();

        FuturesOrder futuresOrder = savePlacedOrder(command, orderId, decision, preview);
        OrderPreview resultPreview = preview;
        FuturesOrder resultOrder = futuresOrder;

        if (decision.executable()) {
            applyFilledOrder(command, orderId, marketSnapshot, preview, decision.executionPrice());
            publishOrderFilled(command, orderId, decision.executionPrice());
        } else {
            Optional<PostSaveLimitFill> postSaveFill = orderPostSaveFillHandler.fillIfMarketable(command, orderId);
            if (postSaveFill.isPresent()) {
                PostSaveLimitFill fill = postSaveFill.orElseThrow();
                resultOrder = fill.order();
                resultPreview = fill.preview();
                publishOrderFilled(command, orderId, fill.order().executionPrice());
            } else {
                pendingLimitOrderBook.addAfterCommit(command.memberId(), futuresOrder);
            }
        }

        return CreateOrderResult.from(resultOrder, resultPreview);
    }

    private FuturesOrder savePlacedOrder(
            CreateOrderCommand command,
            String orderId,
            OrderPlacementDecision decision,
            OrderPreview preview
    ) {
        return orderRepository.save(
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
    }

    private void applyFilledOrder(
            CreateOrderCommand command,
            String orderId,
            MarketSnapshot marketSnapshot,
            OrderPreview preview,
            double executionPrice
    ) {
        orderFillApplier.apply(new FilledOpenOrder(
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
                preview.estimatedInitialMargin()
        ));
    }

    private void publishOrderFilled(CreateOrderCommand command, String orderId, double executionPrice) {
        afterCommitEventPublisher.publish(TradingExecutionEvent.orderFilled(
                command.memberId(),
                orderId,
                command.symbol(),
                command.positionSide(),
                command.marginMode(),
                command.quantity(),
                executionPrice
        ));
    }

    private OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        OrderPreview basePreview = orderPreviewPolicy.preview(
                orderPlacementFactory.openPosition(command),
                marketSnapshot.lastPrice()
        );
        return orderCrossMarginPreviewProjector.project(command, marketSnapshot, basePreview);
    }

    private MarketSnapshot loadMarket(String symbol) {
        return realtimeMarketPriceReader.requireFreshMarket(symbol);
    }

    private void validateOrderType(String orderType) {
        if (!FuturesOrder.isOpenOrderType(orderType)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
