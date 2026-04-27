package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOpenPositionsService {
    private final PositionRepository positionRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final Providers providers;

    @Transactional(readOnly = true)
    public List<PositionSnapshotResult> getPositions(String memberId) {
        return positionRepository.findOpenPositions(memberId).stream()
                .map(this::markToMarketForRead)
                .map(snapshot -> toResult(memberId, snapshot))
                .toList();
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        MarketSnapshot market = providers.connector().marketDataGateway().loadMarket(snapshot.symbol());
        if (market == null) {
            return snapshot;
        }
        return snapshot.markToMarket(market.markPrice());
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
                snapshot.initialMargin(),
                snapshot.roi(),
                pendingCloseQuantity,
                Math.max(0, snapshot.quantity() - pendingCloseQuantity)
        );
    }
}
