package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdatePositionLeverageRequest(
        @NotBlank String symbol,
        @NotBlank String positionSide,
        @NotBlank String marginMode,
        @Min(1) @Max(50) int leverage
) {
}
