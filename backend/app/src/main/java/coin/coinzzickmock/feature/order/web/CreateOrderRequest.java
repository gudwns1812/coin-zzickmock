package coin.coinzzickmock.feature.order.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank String symbol,
        @NotBlank String positionSide,
        @NotBlank String orderType,
        @NotBlank String marginMode,
        @Min(1) @Max(50) int leverage,
        @Positive double quantity,
        Double limitPrice
) {
}
