package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

public record ClosePositionRequest(
        @NotBlank String symbol,
        @Pattern(regexp = "(?i)^(LONG|SHORT)$") String positionSide,
        @Pattern(regexp = "(?i)^(CROSS|ISOLATED)$") String marginMode,
        @Positive double quantity,
        @Pattern(regexp = "(?i)^(MARKET|LIMIT)$") String orderType,
        @Positive Double limitPrice
) {
    @AssertTrue
    public boolean isLimitPricePresentForLimitOrder() {
        return orderType == null || !orderType.equalsIgnoreCase("LIMIT") || limitPrice != null;
    }
}
