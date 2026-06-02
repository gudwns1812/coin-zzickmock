package coin.coinzzickmock.feature.reward.web;

import jakarta.validation.constraints.NotBlank;

public record CreateRedemptionRequest(
        @NotBlank String itemCode,
        @NotBlank String phoneNumber
) {
}
