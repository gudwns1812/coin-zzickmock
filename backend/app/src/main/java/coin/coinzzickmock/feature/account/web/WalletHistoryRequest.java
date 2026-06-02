package coin.coinzzickmock.feature.account.web;

import jakarta.validation.constraints.AssertTrue;
import java.time.Instant;

public record WalletHistoryRequest(
        Instant from,
        Instant to
) {
    @AssertTrue
    public boolean isChronologicalRange() {
        return from == null || to == null || !from.isAfter(to);
    }
}
