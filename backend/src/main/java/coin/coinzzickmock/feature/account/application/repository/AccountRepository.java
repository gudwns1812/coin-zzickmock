package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;

import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(Long memberId);

    default TradingAccount create(TradingAccount account) {
        throw new UnsupportedOperationException("Account creation must use an insert-only repository implementation.");
    }

    default AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount,
            WalletHistorySource source
    ) {
        Optional<TradingAccount> current = findByMemberId(expectedAccount.memberId());
        if (current.isEmpty()) {
            return AccountMutationResult.notFound();
        }
        if (current.orElseThrow().version() != expectedAccount.version()) {
            return AccountMutationResult.staleVersion(current.orElseThrow());
        }
        TradingAccount saved = save(nextAccount.withVersion(expectedAccount.version() + 1), source);
        return AccountMutationResult.updated(1, saved);
    }

    TradingAccount save(TradingAccount account);

    default TradingAccount save(TradingAccount account, WalletHistorySource source) {
        return save(account);
    }
}
