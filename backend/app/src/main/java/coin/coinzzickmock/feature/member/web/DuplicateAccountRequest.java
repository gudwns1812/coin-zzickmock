package coin.coinzzickmock.feature.member.web;

import jakarta.validation.constraints.NotBlank;

public record DuplicateAccountRequest(
        @NotBlank String account
) {
}
