package coin.coinzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.CategoryPageResult;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPageResponseDto {

    private int totalPages;

    private List<CategoryStockResponseDto> stocks;

    public static CategoryPageResponseDto from(CategoryPageResult page) {
        return new CategoryPageResponseDto(
                page.totalPages(),
                page.stocks().stream()
                        .map(CategoryStockResponseDto::from)
                        .toList()
        );
    }

}
