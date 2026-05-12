package coin.coinzzickmock.feature.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WalletHistorySnapshot(
        Long memberId,
        LocalDate snapshotDate,
        BigDecimal baselineWalletBalance,
        BigDecimal walletBalance,
        BigDecimal dailyWalletChange,
        Instant recordedAt
) {
}
