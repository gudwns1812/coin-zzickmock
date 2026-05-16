package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMutationLock {
    private final AccountRepository accountRepository;

    public void lock(Long memberId) {
        accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
