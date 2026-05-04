package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.service.AccountOrderMutationLock;
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
import org.springframework.transaction.annotation.Transactional;

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
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final AccountOrderMutationLock accountOrderMutationLock;

    @Transactional
    public void liquidateBreachedPositions(MarketSummaryResult market) {
        MarketSummaryResult realtimeMarket = freshMarket(market);
        if (realtimeMarket == null) {
            return;
        }
        List<OpenPositionCandidate> candidates = positionRepository.findOpenBySymbol(realtimeMarket.symbol());
        Set<Long> assessedCrossMembers = new HashSet<>();

        for (OpenPositionCandidate candidate : candidates) {
            PositionSnapshot marked = candidate.position().markToMarket(realtimeMarket.markPrice());
            if (marked.isCrossMargin()) {
                if (assessedCrossMembers.add(candidate.memberId())) {
                    liquidateCrossIfNeeded(candidate.memberId(), realtimeMarket);
                }
                continue;
            }

            IsolatedLiquidationAssessment assessment = liquidationPolicy.assessIsolated(marked, realtimeMarket.markPrice());
            if (assessment.breached()) {
                liquidate(candidate.memberId(), marked, realtimeMarket.lastPrice(), realtimeMarket.markPrice());
            }
        }
    }

    private MarketSummaryResult freshMarket(MarketSummaryResult eventMarket) {
        return realtimeMarketPriceReader.freshMarket(eventMarket.symbol())
                .map(prices -> withRealtimePrices(eventMarket, prices))
                .orElse(null);
    }

    private MarketSummaryResult withRealtimePrices(MarketSummaryResult eventMarket, MarketSnapshot prices) {
        return new MarketSummaryResult(
                eventMarket.symbol(),
                eventMarket.displayName(),
                prices.lastPrice(),
                prices.markPrice(),
                prices.indexPrice(),
                eventMarket.fundingRate(),
                eventMarket.change24h(),
                eventMarket.turnover24hUsdt(),
                eventMarket.serverTime(),
                eventMarket.nextFundingAt(),
                eventMarket.fundingIntervalHours()
        );
    }

    private void liquidateCrossIfNeeded(Long memberId, MarketSummaryResult market) {
        TradingAccount account = accountRepository.findByMemberIdForUpdate(memberId)
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

    private void liquidate(Long memberId, PositionSnapshot position, double executionPrice, double markPrice) {
        accountOrderMutationLock.lock(memberId);
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
