package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePositionTpslService {
    private final PositionRepository positionRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final Providers providers;

    @Transactional
    public PositionSnapshotResult update(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode,
            Double takeProfitPrice,
            Double stopLossPrice
    ) {
        PositionSnapshot current = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));
        MarketSnapshot market = providers.connector().marketDataGateway().loadMarket(current.symbol());
        if (market == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND);
        }

        double markPrice = market.markPrice();
        validateTargetPrices(current, takeProfitPrice, stopLossPrice, markPrice);

        PositionSnapshot marked = current.markToMarket(markPrice);
        PositionSnapshot next = marked.withTakeProfitStopLoss(takeProfitPrice, stopLossPrice);
        PositionMutationResult mutationResult = positionRepository.updateWithVersion(memberId, current, next);
        if (!mutationResult.succeeded()) {
            if (mutationResult.status() == PositionMutationResult.Status.NOT_FOUND) {
                throw new CoreException(ErrorCode.POSITION_NOT_FOUND);
            }
            throw new CoreException(ErrorCode.POSITION_CHANGED);
        }

        return toResult(memberId, mutationResult.updatedSnapshot());
    }

    private void validateTargetPrices(
            PositionSnapshot position,
            Double takeProfitPrice,
            Double stopLossPrice,
            double markPrice
    ) {
        validatePositivePrice(takeProfitPrice, "TP 가격을 확인해주세요.");
        validatePositivePrice(stopLossPrice, "SL 가격을 확인해주세요.");

        if (takeProfitPrice != null
                && position.withTakeProfitStopLoss(takeProfitPrice, null).triggersTakeProfit(markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "이미 발동된 TP 가격은 설정할 수 없습니다.");
        }
        if (stopLossPrice != null
                && position.withTakeProfitStopLoss(null, stopLossPrice).triggersStopLoss(markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "이미 발동된 SL 가격은 설정할 수 없습니다.");
        }
    }

    private void validatePositivePrice(Double price, String message) {
        if (price != null && (!Double.isFinite(price) || price <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, message);
        }
    }

    private PositionSnapshotResult toResult(String memberId, PositionSnapshot snapshot) {
        double pendingCloseQuantity = pendingCloseOrderCapReconciler.pendingCloseQuantity(
                memberId,
                snapshot
        );
        return new PositionSnapshotResult(
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode(),
                snapshot.leverage(),
                snapshot.quantity(),
                snapshot.entryPrice(),
                snapshot.markPrice(),
                snapshot.liquidationPrice(),
                snapshot.unrealizedPnl(),
                snapshot.realizedPnl(),
                snapshot.initialMargin(),
                snapshot.roi(),
                pendingCloseQuantity,
                Math.max(0, snapshot.quantity() - pendingCloseQuantity),
                snapshot.takeProfitPrice(),
                snapshot.stopLossPrice()
        );
    }
}
