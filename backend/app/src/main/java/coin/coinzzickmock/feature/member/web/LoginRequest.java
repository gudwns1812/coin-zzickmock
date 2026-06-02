package coin.coinzzickmock.feature.member.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String account,
        @NotBlank String password
) {
}
