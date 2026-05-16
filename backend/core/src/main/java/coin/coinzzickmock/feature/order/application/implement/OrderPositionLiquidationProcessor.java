package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBook;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookHydrator;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.IsolatedLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPositionLiquidationProcessor {
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final LiquidationPolicy liquidationPolicy;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderMutationLock orderMutationLock;
    private final OpenPositionBook openPositionBook;
    private final OpenPositionBookHydrator openPositionBookHydrator;



    @Transactional
    public void liquidateBreachedPositions(MarketSummaryResult market) {
        MarketSummaryResult realtimeMarket = freshMarket(market);
        if (realtimeMarket == null) {
            return;
        }
        liquidateBreachedCandidates(realtimeMarket);
    }

    private void liquidateBreachedCandidates(MarketSummaryResult realtimeMarket) {
        List<OpenPositionCandidate> candidates = liquidationCandidates(realtimeMarket.symbol());
        Set<Long> assessedCrossMembers = new HashSet<>();
        for (OpenPositionCandidate candidate : candidates) {
            liquidateCandidateIfBreached(candidate, realtimeMarket, assessedCrossMembers);
        }
    }

    private List<OpenPositionCandidate> liquidationCandidates(String symbol) {
        OpenPositionBook.Candidates candidates = openPositionBook.candidatesBySymbol(symbol);
        if (candidates.state() == OpenPositionBook.State.UNHYDRATED) {
            openPositionBookHydrator.hydrate();
            candidates = openPositionBook.candidatesBySymbol(symbol);
        } else if (candidates.state() == OpenPositionBook.State.DIRTY) {
            openPositionBookHydrator.rehydrateSymbol(symbol);
            candidates = openPositionBook.candidatesBySymbol(symbol);
            if (candidates.state() == OpenPositionBook.State.DIRTY) {
                return positionRepository.findOpenBySymbol(symbol);
            }
        }
        return candidates.values();
    }

    private void liquidateCandidateIfBreached(
            OpenPositionCandidate candidate,
            MarketSummaryResult market,
            Set<Long> assessedCrossMembers
    ) {
        PositionSnapshot marked = candidate.position().markToMarket(market.markPrice());
        if (marked.isCrossMargin()) {
            liquidateCrossMemberOnce(candidate.memberId(), market, assessedCrossMembers);
            return;
        }
        liquidateIsolatedIfBreached(candidate.memberId(), marked, market);
    }

    private void liquidateCrossMemberOnce(
            Long memberId,
            MarketSummaryResult market,
            Set<Long> assessedCrossMembers
    ) {
        if (assessedCrossMembers.add(memberId)) {
            liquidateCrossIfNeeded(memberId, market);
        }
    }

    private void liquidateIsolatedIfBreached(
            Long memberId,
            PositionSnapshot marked,
            MarketSummaryResult market
    ) {
        IsolatedLiquidationAssessment assessment = liquidationPolicy.assessIsolated(marked, market.markPrice());
        if (assessment.breached()) {
            liquidate(memberId, marked, market.lastPrice(), market.markPrice());
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

        CrossLiquidationAssessment assessment = liquidationPolicy.assessCross(account.walletBalance(), positions);
        if (!assessment.breached()) {
            return;
        }

        assessment.liquidationCandidate()
                .filter(candidate -> candidate.position().symbol().equalsIgnoreCase(market.symbol()))
                .ifPresent(candidate -> liquidate(memberId, candidate.position(), market.lastPrice(), market.markPrice()));
    }

    private void liquidate(Long memberId, PositionSnapshot position, double executionPrice, double markPrice) {
        orderMutationLock.lock(memberId);
        try {
            var result = positionCloseFinalizer.liquidate(memberId, position, markPrice, TAKER_FEE_RATE);
            pendingCloseOrderCapReconciler.reconcile(memberId, position, 0, executionPrice);
            cancelStaleProtectiveOrders(memberId, position);
            publishPositionLiquidated(memberId, position, result.closedQuantity(), markPrice, result.realizedPnl());
        } catch (CoreException exception) {
            if (exception.errorCode() == ErrorCode.POSITION_CHANGED || exception.errorCode() == ErrorCode.POSITION_NOT_FOUND) {
                openPositionBook.evictSymbol(position.symbol());
                openPositionBookHydrator.rehydrateSymbol(position.symbol());
                return;
            }
            throw exception;
        }
    }

    private void publishPositionLiquidated(
            Long memberId,
            PositionSnapshot position,
            double closedQuantity,
            double executionPrice,
            double realizedPnl
    ) {
        afterCommitEventPublisher.publish(TradingExecutionEvent.positionLiquidated(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                closedQuantity,
                executionPrice,
                realizedPnl
        ));
    }

    private void cancelStaleProtectiveOrders(Long memberId, PositionSnapshot position) {
        try {
            staleProtectiveCloseOrderCanceller.cancel(memberId, position);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to cancel stale protective close orders after liquidation. symbol={}, positionSide={}, marginMode={}",
                    position.symbol(),
                    position.positionSide(),
                    position.marginMode(),
                    exception
            );
        }
    }
}
