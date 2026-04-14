package coin.coinzzickmock.core.api.stock.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StockCountRequestDto(
        @NotBlank String stockCode
) {
}
