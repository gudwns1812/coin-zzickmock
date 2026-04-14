package coin.coinzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.StockSummaryResult;

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

    public static CategoryStockResponseDto from(StockSummaryResult stock) {
        return new CategoryStockResponseDto(
                stock.stockName(),
                stock.stockCode(),
                stock.currentPrice(),
                stock.changeRate(),
                stock.sign(),
                stock.changeAmount(),
                stock.stockImage()
        );
    }
}
