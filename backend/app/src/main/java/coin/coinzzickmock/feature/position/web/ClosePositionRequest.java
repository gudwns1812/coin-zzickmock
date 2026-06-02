package coin.coinzzickmock.feature.position.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ClosePositionRequest(
        @NotBlank String symbol,
        @NotBlank String positionSide,
        @NotBlank String marginMode,
        @Positive double quantity,
        @NotBlank String orderType,
        Double limitPrice
) {
}
