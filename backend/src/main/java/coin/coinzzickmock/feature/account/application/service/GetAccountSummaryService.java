package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAccountSummaryService {
    private final AccountRepository accountRepository;
    private final RewardPointRepository rewardPointRepository;

    public GetAccountSummaryService(
            AccountRepository accountRepository,
            RewardPointRepository rewardPointRepository
    ) {
        this.accountRepository = accountRepository;
        this.rewardPointRepository = rewardPointRepository;
    }

    @Transactional(readOnly = true)
    public AccountSummaryResult execute(GetAccountSummaryQuery query) {
        TradingAccount account = accountRepository.findByMemberId(query.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        RewardPointWallet rewardPointWallet = rewardPointRepository.findByMemberId(query.memberId())
                .orElse(new RewardPointWallet(query.memberId(), 0));

        return new AccountSummaryResult(
                account.memberId(),
                account.memberName(),
                account.walletBalance(),
                account.availableMargin(),
                rewardPointWallet.rewardPoint()
        );
    }
}
