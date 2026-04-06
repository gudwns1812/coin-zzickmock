package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.Stock;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {

    private String stockName;

    private String stockCode;

    private String currentPrice;

    private String sign;

    private String changeAmount;

    private String changeRate;

    private String stockImage;

    public static SearchResponseDto from(Stock stock) {
        return new SearchResponseDto(
                stock.getName(),
                stock.getStockCode(),
                stock.getPrice(),
                stock.getSign(),
                stock.getChangeAmount(),
                stock.getChangeRate(),
                stock.getStockImage()
        );
    }
}
