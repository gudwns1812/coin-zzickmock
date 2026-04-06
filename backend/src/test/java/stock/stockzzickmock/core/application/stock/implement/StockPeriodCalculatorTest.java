package stock.stockzzickmock.core.application.stock.implement;

import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.application.stock.implement.result.StockPeriodResult;
import stock.stockzzickmock.core.domain.stock.StockHistory;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StockPeriodCalculatorTest {

    private final StockPeriodCalculator stockPeriodCalculator = new StockPeriodCalculator();

    @Test
    void calculatesSamePeriodValuesAsLegacyLogic() {
        StockHistory stockHistory = StockHistory.builder()
                .stockCode("005930")
                .date(LocalDate.of(2025, 1, 1))
                .type("D")
                .open("69000")
                .high("71000")
                .low("68000")
                .close("70000")
                .volume("1000")
                .volumeAmount("70000000")
                .prevPrice(500)
                .build();

        StockPeriodResult result = stockPeriodCalculator.calculate(stockHistory);

        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.prevPrice()).isEqualTo(69500);
        assertThat(result.openFromPrev()).isEqualTo(-500);
        assertThat(result.closeFromPrev()).isEqualTo(500);
        assertThat(result.highFromPrev()).isEqualTo(1500);
        assertThat(result.lowFromPrev()).isEqualTo(-1500);
    }
}
