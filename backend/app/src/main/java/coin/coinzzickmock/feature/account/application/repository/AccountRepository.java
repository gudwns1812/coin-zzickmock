package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(Long memberId);

    Optional<TradingAccount> findByMemberIdForUpdate(Long memberId);

    List<TradingAccount> findAll();

    TradingAccount create(TradingAccount account);

    AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount
    );
}
