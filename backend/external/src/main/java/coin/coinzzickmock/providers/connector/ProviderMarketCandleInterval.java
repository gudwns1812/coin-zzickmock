package coin.coinzzickmock.providers.connector;

public enum ProviderMarketCandleInterval {
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

    ProviderMarketCandleInterval(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
