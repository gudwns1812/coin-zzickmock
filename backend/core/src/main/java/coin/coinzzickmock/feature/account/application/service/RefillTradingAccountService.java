package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository.LockedAccountRefillState;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.result.AccountRefillResult;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        LocalDate refillDate = datePolicy.currentRefillDate();
        TradingAccount account = accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRefillStateRepository.provisionWeeklyStateIfAbsent(memberId, refillDate);
        LockedAccountRefillState lockedState = accountRefillStateRepository
                .findByMemberIdAndRefillDateForUpdate(memberId, refillDate)
                .orElseThrow(() -> {
                    log.error("Account refill state was not found after provisioning. operation=refill_trading_account");
                    return new CoreException(ErrorCode.INTERNAL_SERVER_ERROR);
                });
        AccountRefillState state = lockedState.state();

        validateRefillable(memberId, account, state);

        AccountRefillState consumedState = lockedState.consumeOne();
        TradingAccount updatedAccount = validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.refillToInitialBalance()
        ));
        afterCommitEventPublisher.publish(WalletBalanceChangedEvent.from(updatedAccount));

        return AccountRefillResult.from(updatedAccount, consumedState.remainingCount());
    }

    private void validateRefillable(Long memberId, TradingAccount account, AccountRefillState state) {
        if (state.remainingCount() <= 0) {
            throw invalid();
        }
        if (!account.refillableToInitialBalance()) {
            throw invalid();
        }
        if (positionRepository.existsOpenByMemberId(memberId)) {
            throw invalid();
        }
        if (orderRepository.existsPendingByMemberId(memberId)) {
            throw invalid();
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

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
