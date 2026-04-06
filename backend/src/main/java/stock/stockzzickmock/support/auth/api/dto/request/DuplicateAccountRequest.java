package stock.stockzzickmock.support.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DuplicateAccountRequest(
        @NotBlank String account
) {
}
