package stock.stockzzickmock.storage.redis.dto;

import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.domain.stock.Stock;

import static org.assertj.core.api.Assertions.assertThat;

class StockDtoTest {

    @Test
    void createsStockDtoFromStock() {
        Stock stock = Stock.builder()
                .stockCode("005930")
                .name("삼성전자")
                .category("전기·전자")
                .price("70000")
                .openPrice("69500")
                .highPrice("70500")
                .lowPrice("69000")
                .marketName("KOSPI")
                .changeAmount("1000")
                .sign("2")
                .changeRate("1.45")
                .volume("1000")
                .volumeValue("90000000")
                .stockImage("image")
                .build();

        StockDto result = StockDto.from(stock);

        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getCategoryName()).isEqualTo("전기·전자");
        assertThat(result.getMarketName()).isEqualTo("KOSPI");
    }
}
