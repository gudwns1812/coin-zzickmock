package coin.coinzzickmock.feature.position.application.close;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.position.application.event.PositionFullyClosedEvent;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionCloseOutcome;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PositionCloseFinalizer {
    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final RewardPointGrantProcessor rewardPointGrantProcessor;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public ClosePositionResult close(
            Long memberId,
            PositionSnapshot position,
            double quantity,
            double markPrice,
            double executionPrice,
            double feeRate,
            String closeReason
    ) {
        if (!Double.isFinite(quantity) || quantity <= 0 || quantity > position.quantity()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        TradingAccount account = accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        PositionCloseOutcome closeOutcome = position.close(quantity, markPrice, executionPrice, feeRate);
        PositionMutationResult mutationResult;
        if (closeOutcome.remainingPosition() == null) {
            mutationResult = positionRepository.deleteWithVersion(memberId, position);
            validateGuardedMutation(mutationResult);
            positionHistoryRepository.save(memberId, toHistory(position, closeOutcome, closeReason));
            afterCommitEventPublisher.publish(new PositionFullyClosedEvent(memberId, position.symbol()));
        } else {
            mutationResult = positionRepository.updateWithVersion(memberId, position, closeOutcome.remainingPosition());
            validateGuardedMutation(mutationResult);
        }

        TradingAccount updatedAccount = validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.settlePositionClose(
                        closeOutcome.grossRealizedPnl(),
                        closeOutcome.closeFee(),
                        closeOutcome.releasedMargin()
                )
        ));
        afterCommitEventPublisher.publish(WalletBalanceChangedEvent.from(updatedAccount));

        RewardPointResult rewardPointResult = rewardPointGrantProcessor.grant(
                new GrantProfitPointCommand(memberId, closeOutcome.netRealizedPnl())
        );

        return ClosePositionResult.from(position, closeOutcome, rewardPointResult);
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

    private void validateGuardedMutation(PositionMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return;
        }
        if (mutationResult.status() == PositionMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.POSITION_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.POSITION_CHANGED);
    }

    private PositionHistory toHistory(
            PositionSnapshot position,
            PositionCloseOutcome closeOutcome,
            String closeReason
    ) {
        return new PositionHistory(
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                position.leverage(),
                closeOutcome.openedAt(),
                closeOutcome.averageEntryPrice(),
                closeOutcome.averageExitPrice(),
                closeOutcome.positionSize(),
                closeOutcome.positionNetRealizedPnl(),
                closeOutcome.accumulatedGrossRealizedPnl(),
                closeOutcome.accumulatedOpenFee(),
                closeOutcome.accumulatedCloseFee(),
                closeOutcome.totalFee(),
                closeOutcome.accumulatedFundingCost(),
                closeOutcome.positionNetRealizedPnl(),
                closeOutcome.roi(position.leverage()),
                Instant.now(),
                closeReason
        );
    }
}
