package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.Stock;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStockResponseDto {

    private String stockName;

    private String stockCode;

    private String currentPrice;

    private String changeRate;

    private String sign;

    private String changeAmount;

    private String stockImage;

    public static CategoryStockResponseDto from(Stock stock) {
        return new CategoryStockResponseDto(
                stock.getName(),
                stock.getStockCode(),
                stock.getPrice(),
                stock.getChangeRate(),
                stock.getSign(),
                stock.getChangeAmount(),
                stock.getStockImage()
        );
    }
}
