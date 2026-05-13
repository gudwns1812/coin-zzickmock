package coin.coinzzickmock.feature.account.application.result;

import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WalletHistoryResult(
        LocalDate snapshotDate,
        BigDecimal walletBalance,
        BigDecimal dailyWalletChange,
        Instant recordedAt
) {
    public static WalletHistoryResult from(WalletHistorySnapshot snapshot) {
        return new WalletHistoryResult(
                snapshot.snapshotDate(),
                snapshot.walletBalance(),
                snapshot.dailyWalletChange(),
                snapshot.recordedAt()
        );
    }
}
