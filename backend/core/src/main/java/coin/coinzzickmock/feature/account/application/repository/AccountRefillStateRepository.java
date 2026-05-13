package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import java.time.LocalDate;
import java.util.Optional;

public interface AccountRefillStateRepository {
    Optional<AccountRefillState> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate);

    void provisionWeeklyStateIfAbsent(Long memberId, LocalDate refillDate);

    AccountRefillState grantExtraRefillCount(Long memberId, LocalDate refillDate, int count);

    Optional<LockedAccountRefillState> findByMemberIdAndRefillDateForUpdate(Long memberId, LocalDate refillDate);

    interface LockedAccountRefillState {
        AccountRefillState state();

        AccountRefillState consumeOne();
    }
}
