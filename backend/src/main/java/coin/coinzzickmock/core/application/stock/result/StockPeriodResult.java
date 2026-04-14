package coin.coinzzickmock.core.application.stock.result;

import java.time.LocalDate;

public record StockPeriodResult(
        String stockCode,
        LocalDate date,
        String type,
        String open,
        String high,
        String low,
        String close,
        String volume,
        String volumeAmount,
        Integer prevPrice,
        Integer openFromPrev,
        Integer closeFromPrev,
        Integer highFromPrev,
        Integer lowFromPrev
) {
}
