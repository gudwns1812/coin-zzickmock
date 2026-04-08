package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.Stock;

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

    public static StockInfoResponseDto from(Stock stock) {
        return new StockInfoResponseDto(
                stock.getStockCode(),
                stock.getName(),
                stock.getCategory(),
                stock.getPrice(),
                stock.getOpenPrice(),
                stock.getHighPrice(),
                stock.getLowPrice(),
                stock.getMarketName(),
                stock.getChangeAmount(),
                stock.getSign(),
                stock.getChangeRate(),
                stock.getVolume(),
                stock.getVolumeValue(),
                stock.getStockImage()
        );
    }
}
