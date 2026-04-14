package coin.coinzzickmock.core.api.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ActiveStockSetRequestDto(
        @NotBlank String source,
        @NotNull List<@NotBlank String> stockCodes
) {
}
