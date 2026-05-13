package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.service.FilledOpenOrderApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationEstimate;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionIdentity;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class CreateOrderService {
    private static final String ORDER_TYPE_LIMIT = "LIMIT";
    private static final String ORDER_TYPE_MARKET = "MARKET";

    private final OrderPreviewPolicy orderPreviewPolicy;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final LiquidationPolicy liquidationPolicy;
    private final FilledOpenOrderApplier filledOpenOrderApplier;
    private final AccountOrderMutationLock accountOrderMutationLock;

    @Transactional(readOnly = true)
    public OrderPreview preview(CreateOrderCommand command) {
        validateOrderType(command.orderType());
        validateExistingPositionInvariants(command);
        return preview(command, loadMarket(command.symbol()));
    }

    @Transactional
    public CreateOrderResult execute(CreateOrderCommand command) {
        accountOrderMutationLock.lock(command.memberId());
        validateOrderType(command.orderType());
        validateExistingPositionInvariants(command);
        MarketSnapshot marketSnapshot = loadMarket(command.symbol());
        OrderPlacementRequest placementRequest = placementRequest(command);
        OrderPlacementDecision decision = orderPlacementPolicy.decide(placementRequest, marketSnapshot.lastPrice());
        OrderPreview preview = preview(command, marketSnapshot);
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

        OrderPreview resultPreview = preview;
        FuturesOrder resultOrder = futuresOrder;
        if (decision.executable()) {
            applyFilledOrder(command, orderId, marketSnapshot, preview, decision.executionPrice());
        } else {
            Optional<PostSaveLimitFill> postSaveFill = fillIfMarketableAfterSave(command, orderId);
            if (postSaveFill.isPresent()) {
                PostSaveLimitFill fill = postSaveFill.orElseThrow();
                resultOrder = fill.order();
                resultPreview = fill.preview();
            }
        }

        return CreateOrderResult.from(resultOrder, resultPreview);
    }

    private Optional<PostSaveLimitFill> fillIfMarketableAfterSave(CreateOrderCommand command, String orderId) {
        if (!ORDER_TYPE_LIMIT.equalsIgnoreCase(command.orderType()) || command.limitPrice() == null) {
            return Optional.empty();
        }

        Optional<MarketSnapshot> refreshedMarketSnapshot = realtimeMarketPriceReader.freshMarket(command.symbol());
        if (refreshedMarketSnapshot.isEmpty()) {
            return Optional.empty();
        }

        MarketSnapshot refreshedMarket = refreshedMarketSnapshot.orElseThrow();
        OrderPlacementDecision refreshedDecision = orderPlacementPolicy.decide(
                placementRequest(command),
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
                preview.estimatedInitialMargin()
        ));
    }

    private OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        OrderPreview basePreview = orderPreviewPolicy.preview(placementRequest(command), marketSnapshot.lastPrice());
        if (!isCrossMargin(command)) {
            return basePreview;
        }

        TradingAccount account = accountRepository.findByMemberId(command.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                account.walletBalance() - basePreview.estimatedFee(),
                positionsAfterPreview(command, marketSnapshot, basePreview),
                command.symbol()
        );
        return new OrderPreview(
                basePreview.feeType(),
                basePreview.estimatedFee(),
                basePreview.estimatedInitialMargin(),
                estimate.liquidationPrice(),
                estimate.liquidationPriceType(),
                basePreview.estimatedEntryPrice(),
                basePreview.executable()
        );
    }

    private boolean isCrossMargin(CreateOrderCommand command) {
        return new PositionIdentity(command.symbol(), command.positionSide(), command.marginMode()).isCrossMargin();
    }

    private List<PositionSnapshot> positionsAfterPreview(
            CreateOrderCommand command,
            MarketSnapshot marketSnapshot,
            OrderPreview preview
    ) {
        PositionSnapshot existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide()
        ).orElse(null);

        PositionSnapshot previewPosition = previewPosition(command, marketSnapshot, preview, existing);

        List<PositionSnapshot> positions = new ArrayList<>(positionRepository.findOpenPositions(command.memberId()).stream()
                .map(position -> markForPreview(command.symbol(), marketSnapshot.markPrice(), position))
                .filter(position -> !Objects.equals(position.stableKey(), previewPosition.stableKey()))
                .toList());
        positions.add(previewPosition);
        return List.copyOf(positions);
    }

    private PositionSnapshot previewPosition(
            CreateOrderCommand command,
            MarketSnapshot marketSnapshot,
            OrderPreview preview,
            PositionSnapshot existing
    ) {
        if (existing == null) {
            return openPreviewPosition(command, marketSnapshot, preview);
        }
        return increasePreviewPosition(command, marketSnapshot, preview, existing);
    }

    private PositionSnapshot openPreviewPosition(
            CreateOrderCommand command,
            MarketSnapshot marketSnapshot,
            OrderPreview preview
    ) {
        return PositionSnapshot.open(
                command.symbol(),
                command.positionSide(),
                command.marginMode(),
                command.leverage(),
                command.quantity(),
                preview.estimatedEntryPrice(),
                marketSnapshot.markPrice(),
                preview.estimatedFee()
        );
    }

    private PositionSnapshot increasePreviewPosition(
            CreateOrderCommand command,
            MarketSnapshot marketSnapshot,
            OrderPreview preview,
            PositionSnapshot existing
    ) {
        return existing.markToMarket(marketSnapshot.markPrice())
                .increase(
                        existing.leverage(),
                        command.quantity(),
                        preview.estimatedEntryPrice(),
                        marketSnapshot.markPrice(),
                        preview.estimatedFee()
                );
    }

    private PositionSnapshot markForPreview(String commandSymbol, double commandMarkPrice, PositionSnapshot position) {
        if (position.symbol().equalsIgnoreCase(commandSymbol)) {
            return position.markToMarket(commandMarkPrice);
        }
        Optional<Double> markPrice = realtimeMarketPriceReader.freshMarkPrice(position.symbol());
        if (markPrice.isEmpty()) {
            log.warn(
                    "Using stale mark price for cross preview estimate. positionSymbol={}, commandSymbol={}, commandMarkPrice={}",
                    position.symbol(),
                    commandSymbol,
                    commandMarkPrice
            );
            return position;
        }
        return position.markToMarket(markPrice.orElseThrow());
    }

    private void validateExistingPositionInvariants(CreateOrderCommand command) {
        Optional<PositionSnapshot> existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide()
        );
        if (existing.isPresent()) {
            PositionSnapshot position = existing.orElseThrow();
            if (!position.marginMode().equalsIgnoreCase(command.marginMode())) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        }

        Optional<PositionSnapshot> symbolMarginPosition = positionRepository.findOpenPositions(command.memberId())
                .stream()
                .filter(candidate -> candidate.symbol().equalsIgnoreCase(command.symbol()))
                .filter(candidate -> candidate.marginMode().equalsIgnoreCase(command.marginMode()))
                .findFirst();
        if (symbolMarginPosition.map(PositionSnapshot::leverage).orElse(command.leverage()) != command.leverage()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private OrderPlacementRequest placementRequest(CreateOrderCommand command) {
        return new OrderPlacementRequest(
                FuturesOrder.PURPOSE_OPEN_POSITION,
                command.positionSide(),
                command.orderType(),
                command.marginMode(),
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
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private record PostSaveLimitFill(FuturesOrder order, OrderPreview preview) {
    }
}
