package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.application.stock.implement.result.StockPeriodResult;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockPeriodResponseDto {
    private String stockCode;

    private LocalDate date;
    private String type;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String volumeAmount;
    private Integer prevPrice;

    private Integer openFromPrev;
    private Integer closeFromPrev;
    private Integer highFromPrev;
    private Integer lowFromPrev;

    public static StockPeriodResponseDto from(StockPeriodResult result) {
        return new StockPeriodResponseDto(
                result.stockCode(),
                result.date(),
                result.type(),
                result.open(),
                result.high(),
                result.low(),
                result.close(),
                result.volume(),
                result.volumeAmount(),
                result.prevPrice(),
                result.openFromPrev(),
                result.closeFromPrev(),
                result.highFromPrev(),
                result.lowFromPrev()
        );
    }
}
