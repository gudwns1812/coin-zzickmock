package coin.coinzzickmock.feature.positionpeek.web;

import jakarta.validation.constraints.NotBlank;

public record PositionPeekRequest(
        @NotBlank String targetToken,
        String idempotencyKey
) {
}
