package coin.coinzzickmock.storage.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.domain.stock.Stock;

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

    public Stock toDomain() {
        return Stock.builder()
                .stockCode(stockCode)
                .name(stockName)
                .category(categoryName)
                .price(price)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .marketName(marketName)
                .changeAmount(changeAmount)
                .sign(sign)
                .changeRate(changeRate)
                .volume(volume)
                .volumeValue(volumeValue)
                .stockImage(stockImage)
                .build();
    }
}
