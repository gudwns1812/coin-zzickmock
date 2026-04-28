package coin.coinzzickmock.feature.account.api;

import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import java.time.Instant;

public record WalletHistoryResponse(
        double walletBalance,
        double availableMargin,
        String sourceType,
        String sourceReference,
        Instant recordedAt
) {
    public static WalletHistoryResponse from(WalletHistoryResult result) {
        return new WalletHistoryResponse(
                result.walletBalance(),
                result.availableMargin(),
                result.sourceType(),
                result.sourceReference(),
                result.recordedAt()
        );
    }
}
