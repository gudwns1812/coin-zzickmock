package stock.stockzzickmock.support.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String zipcode,
        @NotBlank String address,
        String addressDetail
) {
}
