package stock.stockzzickmock.support.auth.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String account,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String phoneNumber,
        @Email @NotBlank String email,
        @Valid AddressRequest address,
        String fgOffset
) {
}
