package coin.coinzzickmock.feature.push.application.dto;

public enum PushStream {
    MARKET("coin:push:market:v1"),
    TRADING("coin:push:trading:v1");

    private final String defaultKey;

    PushStream(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    public String defaultKey() {
        return defaultKey;
    }
}
