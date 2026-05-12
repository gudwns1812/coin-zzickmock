package coin.coinzzickmock.feature.position.domain;

public record PositionIdentity(
        String symbol,
        String positionSide,
        String marginMode
) {
    private static final String POSITION_SIDE_LONG = "LONG";
    private static final String MARGIN_MODE_CROSS = "CROSS";

    public boolean isLong() {
        return POSITION_SIDE_LONG.equalsIgnoreCase(positionSide);
    }

    public boolean isCrossMargin() {
        return MARGIN_MODE_CROSS.equalsIgnoreCase(marginMode);
    }

    public String stableKey() {
        return String.join(":", symbol, positionSide, marginMode);
    }
}
