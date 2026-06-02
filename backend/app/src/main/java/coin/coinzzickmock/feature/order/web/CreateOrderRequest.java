package coin.coinzzickmock.feature.order.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

public record CreateOrderRequest(
        @NotBlank String symbol,
        @Pattern(regexp = "(?i)^(LONG|SHORT)$") String positionSide,
        @Pattern(regexp = "(?i)^(MARKET|LIMIT)$") String orderType,
        @Pattern(regexp = "(?i)^(CROSS|ISOLATED)$") String marginMode,
        @Min(1) @Max(50) int leverage,
        @Positive double quantity,
        @Positive Double limitPrice
) {
    @AssertTrue
    public boolean isLimitPricePresentForLimitOrder() {
        return orderType == null || !orderType.equalsIgnoreCase("LIMIT") || limitPrice != null;
    }
}
