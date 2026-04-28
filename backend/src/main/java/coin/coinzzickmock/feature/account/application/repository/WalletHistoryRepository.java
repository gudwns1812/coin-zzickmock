package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import java.time.Instant;
import java.util.List;

public interface WalletHistoryRepository {
    void record(TradingAccount account, WalletHistorySource source, Instant recordedAt);

    List<WalletHistorySnapshot> findByMemberIdBetween(String memberId, Instant fromInclusive, Instant toInclusive);
}
