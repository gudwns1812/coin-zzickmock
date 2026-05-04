package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import java.time.LocalDate;
import java.util.Optional;

public interface AccountRefillStateRepository {
    Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate);

    AccountRefillState ensureByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate);

    Optional<AccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate);

    AccountRefillState save(AccountRefillState state);
}
