package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import java.util.Optional;

public enum BitgetWebSocketChannel {
    TRADE("trade", null),
    TICKER("ticker", null),
    CANDLE_1M("candle1m", ProviderMarketCandleInterval.ONE_MINUTE),
    CANDLE_1H("candle1H", ProviderMarketCandleInterval.ONE_HOUR);

    private final String value;
    private final ProviderMarketCandleInterval interval;

    BitgetWebSocketChannel(String value, ProviderMarketCandleInterval interval) {
        this.value = value;
        this.interval = interval;
    }

    public String value() {
        return value;
    }

    public ProviderMarketCandleInterval interval() {
        if (interval == null) {
            throw new IllegalStateException("channel does not represent a candle interval");
        }
        return interval;
    }

    public boolean isCandle() {
        return interval != null;
    }

    public static Optional<BitgetWebSocketChannel> from(String value) {
        for (BitgetWebSocketChannel channel : values()) {
            if (channel.value.equals(value)) {
                return Optional.of(channel);
            }
        }
        return Optional.empty();
    }
}
