package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdatePositionTpslRequest(
        @NotBlank String symbol,
        @NotBlank String positionSide,
        @NotBlank String marginMode,
        @Positive Double takeProfitPrice,
        @Positive Double stopLossPrice
) {
}
