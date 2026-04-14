package coin.coinzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.StockInfoResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockInfoResponseDto {

    private String stockCode;
    private String stockName;
    private String categoryName;
    private String price;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private String marketName;
    private String changeAmount;
    private String sign;
    private String changeRate;
    private String volume;
    private String volumeValue;
    private String stockImage;

    public static StockInfoResponseDto from(StockInfoResult stock) {
        return new StockInfoResponseDto(
                stock.stockCode(),
                stock.stockName(),
                stock.categoryName(),
                stock.price(),
                stock.openPrice(),
                stock.highPrice(),
                stock.lowPrice(),
                stock.marketName(),
                stock.changeAmount(),
                stock.sign(),
                stock.changeRate(),
                stock.volume(),
                stock.volumeValue(),
                stock.stockImage()
        );
    }
}
