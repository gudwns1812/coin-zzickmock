package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.IsolatedLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionLiquidationProcessor {
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final LiquidationPolicy liquidationPolicy;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public void liquidateBreachedPositions(MarketSummaryResult market) {
        List<OpenPositionCandidate> candidates = positionRepository.findOpenBySymbol(market.symbol());
        Set<String> assessedCrossMembers = new HashSet<>();

        for (OpenPositionCandidate candidate : candidates) {
            PositionSnapshot marked = candidate.position().markToMarket(market.markPrice());
            if (marked.isCrossMargin()) {
                if (assessedCrossMembers.add(candidate.memberId())) {
                    liquidateCrossIfNeeded(candidate.memberId(), market);
                }
                continue;
            }

            IsolatedLiquidationAssessment assessment = liquidationPolicy.assessIsolated(marked, market.markPrice());
            if (assessment.breached()) {
                liquidate(candidate.memberId(), marked, market.lastPrice(), market.markPrice());
            }
        }
    }

    private void liquidateCrossIfNeeded(String memberId, MarketSummaryResult market) {
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        List<PositionSnapshot> positions = positionRepository.findOpenPositions(memberId).stream()
                .map(position -> position.symbol().equalsIgnoreCase(market.symbol())
                        ? position.markToMarket(market.markPrice())
                        : position)
                .toList();

        CrossLiquidationAssessment assessment = liquidationPolicy.assessCross(account.availableMargin(), positions);
        if (!assessment.breached()) {
            return;
        }

        assessment.liquidationCandidate()
                .filter(candidate -> candidate.position().symbol().equalsIgnoreCase(market.symbol()))
                .ifPresent(candidate -> liquidate(memberId, candidate.position(), market.lastPrice(), market.markPrice()));
    }

    private void liquidate(String memberId, PositionSnapshot position, double executionPrice, double markPrice) {
        var result = positionCloseFinalizer.close(
                memberId,
                position,
                position.quantity(),
                markPrice,
                executionPrice,
                TAKER_FEE_RATE,
                PositionHistory.CLOSE_REASON_LIQUIDATION
        );
        pendingCloseOrderCapReconciler.reconcile(memberId, position, 0, executionPrice);
        afterCommitEventPublisher.publish(TradingExecutionEvent.positionLiquidated(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                result.closedQuantity(),
                executionPrice,
                result.realizedPnl()
        ));
    }
}
