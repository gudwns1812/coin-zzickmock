package coin.coinzzickmock.feature.account.application.refill;

import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.result.AccountRefillCreditResult;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountRefillCreditProcessor {
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final AccountRefillDatePolicy datePolicy;

    @Transactional(propagation = Propagation.MANDATORY)
    public AccountRefillCreditResult addCurrentWeekCount(Long memberId, int count) {
        AccountRefillState saved = accountRefillStateRepository.grantExtraRefillCount(
                memberId,
                datePolicy.currentRefillDate(),
                count
        );
        return AccountRefillCreditResult.from(saved);
    }
}
