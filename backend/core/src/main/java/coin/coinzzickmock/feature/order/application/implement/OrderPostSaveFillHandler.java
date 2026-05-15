package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.implement.OrderFillApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPostSaveFillHandler {
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final OrderPreviewPolicy orderPreviewPolicy;
    private final OrderRepository orderRepository;
    private final OrderPlacementFactory orderPlacementFactory;
    private final OrderCrossMarginPreviewProjector crossMarginPreviewProjector;
    private final OrderFillApplier orderFillApplier;

    public Optional<PostSaveLimitFill> fillIfMarketable(CreateOrderCommand command, String orderId) {
        if (!FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(command.orderType()) || command.limitPrice() == null) {
            return Optional.empty();
        }

        Optional<MarketSnapshot> refreshedMarketSnapshot = realtimeMarketPriceReader.freshMarket(command.symbol());
        if (refreshedMarketSnapshot.isEmpty()) {
            return Optional.empty();
        }

        MarketSnapshot refreshedMarket = refreshedMarketSnapshot.orElseThrow();
        OrderPlacementDecision refreshedDecision = orderPlacementPolicy.decide(
                orderPlacementFactory.openPosition(command),
                refreshedMarket.lastPrice()
        );
        if (!refreshedDecision.executable()) {
            return Optional.empty();
        }

        OrderPreview refreshedPreview = preview(command, refreshedMarket);
        Optional<FuturesOrder> claimed = orderRepository.claimPendingFill(
                command.memberId(),
                orderId,
                refreshedDecision.executionPrice(),
                refreshedDecision.feeType(),
                refreshedPreview.estimatedFee()
        );
        if (claimed.isEmpty()) {
            return Optional.empty();
        }

        applyFilledOrder(command, orderId, refreshedMarket, refreshedPreview, refreshedDecision.executionPrice());
        return Optional.of(new PostSaveLimitFill(claimed.orElseThrow(), refreshedPreview));
    }

    private OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        OrderPreview basePreview = orderPreviewPolicy.preview(
                orderPlacementFactory.openPosition(command),
                marketSnapshot.lastPrice()
        );
        return crossMarginPreviewProjector.project(command, marketSnapshot, basePreview);
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

    public record PostSaveLimitFill(FuturesOrder order, OrderPreview preview) {
    }
}
