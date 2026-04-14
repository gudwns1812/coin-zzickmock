package coin.coinzzickmock.core.domain.stock;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockHistory {

    private final Long id;
    private final String stockCode;
    private final LocalDate date;
    private String type;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String volumeAmount;
    private Integer prevPrice;

    public void updateHistory(
            String type,
            String open,
            String high,
            String low,
            String close,
            String volume,
            String volumeAmount,
            Integer prevPrice
    ) {
        this.type = type;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.volumeAmount = volumeAmount;
        this.prevPrice = prevPrice;
    }
}
