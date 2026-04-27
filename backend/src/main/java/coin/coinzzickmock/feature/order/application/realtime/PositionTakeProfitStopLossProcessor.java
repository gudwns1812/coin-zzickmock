package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionTakeProfitStopLossProcessor {
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final PositionRepository positionRepository;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public void closeTriggeredPositions(MarketSummaryResult market) {
        List<OpenPositionCandidate> candidates = positionRepository.findOpenBySymbol(market.symbol());
        for (OpenPositionCandidate candidate : candidates) {
            closeIfTriggered(candidate.memberId(), candidate.position(), market);
        }
    }

    private void closeIfTriggered(String memberId, PositionSnapshot position, MarketSummaryResult market) {
        PositionSnapshot marked = position.markToMarket(market.markPrice());
        String closeReason = marked.triggeredCloseReason(market.markPrice());
        if (closeReason == null) {
            return;
        }

        var result = positionCloseFinalizer.close(
                memberId,
                marked,
                marked.quantity(),
                market.markPrice(),
                market.lastPrice(),
                TAKER_FEE_RATE,
                closeReason
        );
        pendingCloseOrderCapReconciler.reconcile(memberId, marked, 0, market.lastPrice());
        afterCommitEventPublisher.publish(TradingExecutionEvent.positionClosedByTrigger(
                memberId,
                marked.symbol(),
                marked.positionSide(),
                marked.marginMode(),
                result.closedQuantity(),
                market.lastPrice(),
                result.realizedPnl(),
                closeReason
        ));
    }
}
