package coin.coinzzickmock.support.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank String memberId
) {
}
