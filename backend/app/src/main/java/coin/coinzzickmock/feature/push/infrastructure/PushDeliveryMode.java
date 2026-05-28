package coin.coinzzickmock.feature.push.infrastructure;

public enum PushDeliveryMode {
    DISABLED,
    SHADOW,
    DUAL,
    ENABLED;

    public boolean publishesToRedis() {
        return this == SHADOW || this == DUAL || this == ENABLED;
    }

    public boolean keepsInAppFanOut() {
        return this == DISABLED || this == SHADOW || this == DUAL;
    }

    public static PushDeliveryMode from(String value) {
        if (value == null || value.isBlank()) {
            return SHADOW;
        }
        return PushDeliveryMode.valueOf(value.trim().toUpperCase());
    }
}
