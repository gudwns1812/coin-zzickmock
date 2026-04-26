package coin.coinzzickmock.feature.leaderboard.domain;

import java.util.Arrays;
import java.util.Optional;

public enum LeaderboardMode {
    PROFIT_RATE("profitRate"),
    WALLET_BALANCE("walletBalance");

    private final String value;

    LeaderboardMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public double score(LeaderboardEntry entry) {
        return this == PROFIT_RATE ? entry.profitRate() : entry.walletBalance();
    }

    public static Optional<LeaderboardMode> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(PROFIT_RATE);
        }
        return Arrays.stream(values())
                .filter(mode -> mode.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
