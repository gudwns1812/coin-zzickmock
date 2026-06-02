package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePositionLeverageRequest(
        @NotBlank String symbol,
        @Pattern(regexp = "(?i)^(LONG|SHORT)$") String positionSide,
        @Pattern(regexp = "(?i)^(CROSS|ISOLATED)$") String marginMode,
        @Min(1) @Max(50) int leverage
) {
}
