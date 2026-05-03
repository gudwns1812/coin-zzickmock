package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradingAccountProvisioningService {
    private final AccountRepository accountRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public TradingAccount openForRegisteredMember(Long memberId, String memberEmail, String memberName) {
        return openDefaultAccount(memberId, memberEmail, memberName);
    }

    @Transactional
    public TradingAccount openForSeedIfMissing(Long memberId, String memberEmail, String memberName) {
        return accountRepository.findByMemberId(memberId)
                .orElseGet(() -> openDefaultAccount(memberId, memberEmail, memberName));
    }

    private TradingAccount openDefaultAccount(Long memberId, String memberEmail, String memberName) {
        TradingAccount account = accountRepository.create(TradingAccount.openDefault(memberId, memberEmail, memberName));
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(memberId));
        return account;
    }
}
