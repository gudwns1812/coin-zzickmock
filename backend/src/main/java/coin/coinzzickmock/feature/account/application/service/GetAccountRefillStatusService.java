package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountRefillStatusResult;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAccountRefillStatusService {
    private final AccountRepository accountRepository;
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final AccountRefillDatePolicy datePolicy;

    @Transactional(readOnly = true)
    public AccountRefillStatusResult get(Long memberId) {
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountRefillState state = accountRefillStateRepository
                .findByMemberIdAndRefillDate(memberId, datePolicy.today())
                .orElse(AccountRefillState.daily(memberId, datePolicy.today()));
        String disabledReason = disabledReason(memberId, account, state);

        return new AccountRefillStatusResult(
                state.remainingCount(),
                disabledReason == null,
                disabledReason,
                BigDecimal.valueOf(TradingAccount.INITIAL_WALLET_BALANCE),
                BigDecimal.valueOf(TradingAccount.INITIAL_AVAILABLE_MARGIN),
                datePolicy.nextResetAt()
        );
    }

    private String disabledReason(Long memberId, TradingAccount account, AccountRefillState state) {
        if (state.remainingCount() <= 0) {
            return "사용 가능한 리필 횟수가 없습니다.";
        }
        if (!account.refillableToInitialBalance()) {
            return "이미 100,000 USDT 기준 잔고에 도달했습니다.";
        }
        if (positionRepository.existsOpenByMemberId(memberId)) {
            return "열린 포지션이 있을 때는 리필할 수 없습니다.";
        }
        if (orderRepository.existsPendingByMemberId(memberId)) {
            return "대기 중인 주문이 있을 때는 리필할 수 없습니다.";
        }
        return null;
    }
}
