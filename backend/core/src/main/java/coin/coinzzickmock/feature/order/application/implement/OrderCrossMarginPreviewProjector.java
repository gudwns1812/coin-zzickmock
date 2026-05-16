package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.dto.CreateOrderCommand;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationEstimate;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionIdentity;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCrossMarginPreviewProjector {
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final LiquidationPolicy liquidationPolicy;

    public OrderPreview project(CreateOrderCommand command, MarketSnapshot marketSnapshot, OrderPreview basePreview) {
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
}
