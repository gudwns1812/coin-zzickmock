package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;

import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(String memberId);

    TradingAccount save(TradingAccount account);

    default TradingAccount save(TradingAccount account, WalletHistorySource source) {
        return save(account);
    }
}
