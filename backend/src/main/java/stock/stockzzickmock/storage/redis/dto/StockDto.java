package stock.stockzzickmock.storage.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.Stock;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {
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

    public static StockDto from(Stock stock) {
        return new StockDto(
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
