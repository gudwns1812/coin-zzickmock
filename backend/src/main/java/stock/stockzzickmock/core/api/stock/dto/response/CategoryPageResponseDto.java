package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.application.stock.implement.result.CategoryStocksPage;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPageResponseDto {

    private int totalPages;

    private List<CategoryStockResponseDto> stocks;

    public static CategoryPageResponseDto of(CategoryStocksPage page) {
        return new CategoryPageResponseDto(
                page.totalPages(),
                page.stocks().stream()
                        .map(CategoryStockResponseDto::from)
                        .toList()
        );
    }

}
