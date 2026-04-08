package stock.stockzzickmock.storage.db.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.stock.StockHistory;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

@Entity(name = "StockHistory")
@Getter
@Table(name = "stock_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StockHistoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "date_type")
    private String type;

    @Column(name = "open_price")
    private String open;

    @Column(name = "high_price")
    private String high;

    @Column(name = "low_price")
    private String low;

    @Column(name = "close_price")
    private String close;

    @Column(name = "volume")
    private String volume;

    @Column(name = "volume_amount")
    private String volumeAmount;

    @Column(name = "prev_price")
    private Integer prevPrice;

    public StockHistory toDomain() {
        return StockHistory.builder()
                .id(id)
                .stockCode(stockCode)
                .date(date)
                .type(type)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .volumeAmount(volumeAmount)
                .prevPrice(prevPrice)
                .build();
    }
}
