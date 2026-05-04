package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.result.AccountRefillResult;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefillTradingAccountService {
    private final AccountRepository accountRepository;
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final AccountRefillDatePolicy datePolicy;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public AccountRefillResult refill(Long memberId) {
        AccountRefillState state = accountRefillStateRepository
                .ensureByMemberIdAndRefillDateForUpdate(memberId, datePolicy.today());
        TradingAccount account = accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateRefillable(memberId, account, state);

        AccountRefillState savedState = accountRefillStateRepository.save(state.consumeOne());
        TradingAccount updatedAccount = validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.refillToInitialBalance()
        ));
        afterCommitEventPublisher.publish(WalletBalanceChangedEvent.from(updatedAccount));

        return new AccountRefillResult(
                updatedAccount.walletBalance(),
                updatedAccount.availableMargin(),
                savedState.remainingCount()
        );
    }

    private void validateRefillable(Long memberId, TradingAccount account, AccountRefillState state) {
        if (state.remainingCount() <= 0) {
            throw invalid("사용 가능한 리필 횟수가 없습니다.");
        }
        if (!account.refillableToInitialBalance()) {
            throw invalid("이미 100,000 USDT 기준 잔고에 도달했습니다.");
        }
        if (positionRepository.existsOpenByMemberId(memberId)) {
            throw invalid("열린 포지션이 있을 때는 리필할 수 없습니다.");
        }
        if (orderRepository.existsPendingByMemberId(memberId)) {
            throw invalid("대기 중인 주문이 있을 때는 리필할 수 없습니다.");
        }
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

    private CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
