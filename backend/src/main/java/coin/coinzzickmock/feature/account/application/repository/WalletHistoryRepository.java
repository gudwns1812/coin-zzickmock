package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WalletHistoryRepository {
    void createBaselineIfAbsent(TradingAccount account, LocalDate snapshotDate);

    void updateCurrentDay(TradingAccount account, LocalDate snapshotDate);

    Optional<WalletHistorySnapshot> findLatestBefore(Long memberId, LocalDate snapshotDate);

    List<WalletHistorySnapshot> findByMemberIdBetween(Long memberId, LocalDate fromInclusive, LocalDate toInclusive);
}
