package coin.coinzzickmock.feature.account.application.result;

import java.time.Instant;

public record WalletHistoryResult(
        double walletBalance,
        double availableMargin,
        String sourceType,
        String sourceReference,
        Instant recordedAt
) {
}
