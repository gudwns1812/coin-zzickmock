package coin.coinzzickmock.feature.account.application.refill;

import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.result.AccountRefillCreditResult;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountRefillCreditProcessor {
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final AccountRefillDatePolicy datePolicy;

    @Transactional
    public AccountRefillCreditResult addTodayCount(Long memberId, int count) {
        AccountRefillState current = accountRefillStateRepository
                .ensureByMemberIdAndRefillDateForUpdate(memberId, datePolicy.today());
        AccountRefillState saved = accountRefillStateRepository.save(current.addCount(count));
        return new AccountRefillCreditResult(saved.remainingCount());
    }
}
