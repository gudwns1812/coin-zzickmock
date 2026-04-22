package coin.coinzzickmock.feature.market.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public enum MarketCandleInterval {
    ONE_MINUTE("1m"),
    THREE_MINUTES("3m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    ONE_HOUR("1h"),
    FOUR_HOURS("4h"),
    TWELVE_HOURS("12h"),
    ONE_DAY("1D"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M");

    private final String value;

    MarketCandleInterval(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MarketCandleInterval from(String value) {
        for (MarketCandleInterval interval : values()) {
            if (interval.value.equalsIgnoreCase(value)) {
                return interval;
            }
        }

        throw new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
