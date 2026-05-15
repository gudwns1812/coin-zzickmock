package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.position.application.query.PositionSnapshotResultAssembler;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookWriter;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePositionLeverageService {
    private static final int MIN_LEVERAGE = 1;
    private static final int MAX_LEVERAGE = 50;
    private static final String POSITION_SIDE_LONG = "LONG";
    private static final String POSITION_SIDE_SHORT = "SHORT";

    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final PositionSnapshotResultAssembler positionSnapshotResultAssembler;
    private final OpenPositionBookWriter openPositionBookWriter;


    @Transactional
    public PositionSnapshotResult update(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            int leverage
    ) {
        validateLeverage(leverage);
        List<PositionSnapshot> targets = findLeverageTargets(memberId, symbol, marginMode);
        PositionSnapshot responseTarget = selectResponseTarget(targets, positionSide);

        if (targets.stream().allMatch(position -> position.leverage() == leverage)) {
            return positionSnapshotResultAssembler.assemble(memberId, responseTarget);
        }

        rejectPendingOpenOrders(memberId, symbol);

        List<PositionSnapshot> changedTargets = targets.stream()
                .filter(position -> position.leverage() != leverage)
                .toList();
        List<PositionSnapshot> nextPositions = changedTargets.stream()
                .map(position -> position.withLeverage(leverage))
                .toList();
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        TradingAccount adjusted = account.adjustAvailableMarginForLeverageChange(
                sumInitialMargin(changedTargets),
                sumInitialMargin(nextPositions)
        );

        TradingAccount updatedAccount = validateAccountMutation(accountRepository.updateWithVersion(
                account,
                adjusted
        ));

        PositionSnapshot updatedResponseTarget = responseTarget;
        for (int index = 0; index < changedTargets.size(); index++) {
            PositionMutationResult mutation = positionRepository.updateWithVersion(
                    memberId,
                    changedTargets.get(index),
                    nextPositions.get(index)
            );
            if (!mutation.succeeded()) {
                throw new CoreException(ErrorCode.POSITION_CHANGED);
            }
            openPositionBookWriter.replaceAfterCommit(memberId, mutation.updatedSnapshot());
            if (samePosition(mutation.updatedSnapshot(), responseTarget)) {
                updatedResponseTarget = mutation.updatedSnapshot();
            }
        }

        afterCommitEventPublisher.publish(WalletBalanceChangedEvent.from(updatedAccount));
        return positionSnapshotResultAssembler.assemble(memberId, updatedResponseTarget, updatedAccount.walletBalance());
    }

    private List<PositionSnapshot> findLeverageTargets(
            Long memberId,
            String symbol,
            String marginMode
    ) {
        List<PositionSnapshot> targets = positionRepository.findOpenPositions(memberId)
                .stream()
                .filter(position -> hasSymbolAndMarginMode(position, symbol, marginMode))
                .toList();
        if (targets.isEmpty()) {
            throw new CoreException(ErrorCode.POSITION_NOT_FOUND);
        }
        return targets;
    }

    private PositionSnapshot selectResponseTarget(List<PositionSnapshot> targets, String positionSide) {
        return targets.stream()
                .filter(position -> hasPositionSide(position, positionSide))
                .findFirst()
                .orElseGet(() -> targets.get(0));
    }

    private double sumInitialMargin(List<PositionSnapshot> positions) {
        return positions.stream()
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
    }

    private boolean samePosition(PositionSnapshot left, PositionSnapshot right) {
        return hasSymbolAndMarginMode(left, right.symbol(), right.marginMode())
                && hasPositionSide(left, right.positionSide());
    }

    private boolean hasSymbolAndMarginMode(PositionSnapshot position, String symbol, String marginMode) {
        return position.symbol().equalsIgnoreCase(symbol)
                && position.marginMode().equalsIgnoreCase(marginMode);
    }

    private boolean hasPositionSide(PositionSnapshot position, String positionSide) {
        return position.positionSide().equalsIgnoreCase(positionSide);
    }

    private TradingAccount validateAccountMutation(AccountMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return mutationResult.updatedAccount();
        }
        if (mutationResult.status() == AccountMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.ACCOUNT_CHANGED);
    }

    private void validateLeverage(int leverage) {
        if (leverage < MIN_LEVERAGE || leverage > MAX_LEVERAGE) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void rejectPendingOpenOrders(Long memberId, String symbol) {
        List.of(POSITION_SIDE_LONG, POSITION_SIDE_SHORT).forEach(positionSide -> {
            if (!orderRepository.findPendingOpenOrders(memberId, symbol, positionSide).isEmpty()) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        });
    }
}
