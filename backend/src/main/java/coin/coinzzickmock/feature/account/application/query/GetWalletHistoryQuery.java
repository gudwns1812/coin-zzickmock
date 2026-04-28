package coin.coinzzickmock.feature.account.application.query;

import java.time.Instant;

public record GetWalletHistoryQuery(
        String memberId,
        Instant from,
        Instant to
) {
}
