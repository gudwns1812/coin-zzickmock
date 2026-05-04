package coin.coinzzickmock.feature.account.web;

import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WalletHistoryResponse(
        LocalDate snapshotDate,
        BigDecimal walletBalance,
        BigDecimal dailyWalletChange,
        Instant recordedAt
) {
    public static WalletHistoryResponse from(WalletHistoryResult result) {
        return new WalletHistoryResponse(
                result.snapshotDate(),
                result.walletBalance(),
                result.dailyWalletChange(),
                result.recordedAt()
        );
    }
}
