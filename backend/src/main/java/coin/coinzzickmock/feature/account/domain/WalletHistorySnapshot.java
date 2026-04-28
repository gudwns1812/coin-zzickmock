package coin.coinzzickmock.feature.account.domain;

import java.time.Instant;

public record WalletHistorySnapshot(
        String memberId,
        double walletBalance,
        double availableMargin,
        String sourceType,
        String sourceReference,
        Instant recordedAt
) {
}
