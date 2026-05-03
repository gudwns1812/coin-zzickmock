package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;

import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(Long memberId);

    default TradingAccount create(TradingAccount account) {
        throw new UnsupportedOperationException("Account creation must use an insert-only repository implementation.");
    }

    TradingAccount save(TradingAccount account);

    default TradingAccount save(TradingAccount account, WalletHistorySource source) {
        return save(account);
    }
}
