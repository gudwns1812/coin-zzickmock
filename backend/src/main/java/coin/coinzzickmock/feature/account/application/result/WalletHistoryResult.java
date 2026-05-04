package coin.coinzzickmock.feature.account.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WalletHistoryResult(
        LocalDate snapshotDate,
        BigDecimal walletBalance,
        BigDecimal dailyWalletChange,
        Instant recordedAt
) {
}
