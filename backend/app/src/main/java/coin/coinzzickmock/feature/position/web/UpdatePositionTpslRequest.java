package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record UpdatePositionTpslRequest(
        @NotBlank String symbol,
        @Pattern(regexp = "(?i)^(LONG|SHORT)$") String positionSide,
        @Pattern(regexp = "(?i)^(CROSS|ISOLATED)$") String marginMode,
        @Positive Double takeProfitPrice,
        @Positive Double stopLossPrice
) {
}
