package stock.stockzzickmock.storage.db.stock.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

@Entity(name = "Stock")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public Stock toDomain() {
        return Stock.builder()
                .id(id)
                .stockCode(stockCode)
                .name(name)
                .price(price)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .marketName(marketName)
                .stockImage(stockImage)
                .changeAmount(changeAmount)
                .sign(sign)
                .changeRate(changeRate)
                .volume(volume)
                .volumeValue(volumeValue)
                .stockSearchCount(stockSearchCount)
                .category(category)
                .build();
    }
}
