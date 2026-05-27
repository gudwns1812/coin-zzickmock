package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradingAccountProvisioningService {
    private final AccountRepository accountRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public TradingAccount openForSeedIfMissing(Long memberId, String memberEmail, String memberName, String nickname) {
        return accountRepository.findByMemberId(memberId)
                .orElseGet(() -> openForSeedOrFindExisting(memberId, memberEmail, memberName, nickname));
    }

    private TradingAccount openForSeedOrFindExisting(
            Long memberId,
            String memberEmail,
            String memberName,
            String nickname
    ) {
        try {
            return openDefaultAccount(memberId, memberEmail, memberName, nickname);
        } catch (DataIntegrityViolationException exception) {
            return accountRepository.findByMemberId(memberId)
                    .orElseThrow(() -> exception);
        }
    }

    private TradingAccount openDefaultAccount(Long memberId, String memberEmail, String memberName, String nickname) {
        TradingAccount account = accountRepository.create(TradingAccount.openDefault(memberId, memberEmail, memberName));
        afterCommitEventPublisher.publish(TradingAccountOpenedEvent.from(account, nickname));
        return account;
    }
}
