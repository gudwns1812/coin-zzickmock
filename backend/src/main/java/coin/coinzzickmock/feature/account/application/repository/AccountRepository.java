package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(Long memberId);

    default Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
        throw new UnsupportedOperationException("findByMemberIdForUpdate requires a pessimistic locking implementation");
    }

    default List<TradingAccount> findAll() {
        throw new UnsupportedOperationException("findAll is not supported by this repository");
    }

    TradingAccount create(TradingAccount account);

    AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount
    );
}
