package stock.stockzzickmock.core.domain.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stock extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "stock_name")
    private String name;

    @Column(name = "stock_price")
    private String price;

    @Column(name = "open_price")
    private String openPrice;

    @Column(name = "high_price")
    private String highPrice;

    @Column(name = "low_price")
    private String lowPrice;

    @Column(name = "market_name")
    private String marketName;

    @Column(name = "stock_image")
    private String stockImage;

    @Column(name = "change_price")
    private String changeAmount;

    @Column(name = "sign")
    private String sign;

    @Column(name = "change_rate")
    private String changeRate;

    private String volume;

    @Column(name = "volume_value")
    private String volumeValue;

    @Column(name = "stock_search_count")
    private Integer stockSearchCount;
    private String category;

    //== 생성 메서드 ==//
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


    //== 비즈니스 로직 ==//
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
