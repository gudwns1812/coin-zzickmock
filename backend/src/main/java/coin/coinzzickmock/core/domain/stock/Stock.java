package coin.coinzzickmock.core.domain.stock;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Stock {

    private final Long id;
    private final String stockCode;
    private final String name;
    private String price;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private final String marketName;
    private String stockImage;
    private String changeAmount;
    private String sign;
    private String changeRate;
    private String volume;
    private String volumeValue;
    private Integer stockSearchCount;
    private final String category;

    public static Stock createStock(String stockCode, String name, Integer price, String marketName, String category) {
        return Stock.builder()
                .stockCode(stockCode)
                .name(name)
                .price(String.valueOf(price))
                .marketName(marketName)
                .category(category)
                .stockSearchCount(0)
                .build();
    }

    public void incrementStockSearchCount() {
        if (stockSearchCount == null) {
            stockSearchCount = 0;
        }
        stockSearchCount++;
    }

    public void updateStockPrice(String price, String changePrice, String sign, String changeRate) {
        this.price = price;
        this.changeRate = changeRate;
        this.changeAmount = changePrice;
        this.sign = sign;
    }
}
