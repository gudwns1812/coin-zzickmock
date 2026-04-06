package stock.stockzzickmock.core.api.stock.dto.response;

import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.application.stock.implement.result.StockPeriodResult;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.redis.dto.KisPopularRedisDto;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StockResponseDtoFactoryTest {

    @Test
    void createsSearchResponseFromStock() {
        Stock stock = stock();

        SearchResponseDto result = SearchResponseDto.from(stock);

        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getStockName()).isEqualTo("삼성전자");
    }

    @Test
    void createsCategoryResponseFromStock() {
        Stock stock = stock();

        CategoryStockResponseDto result = CategoryStockResponseDto.from(stock);

        assertThat(result.getCurrentPrice()).isEqualTo("70000");
        assertThat(result.getStockImage()).isEqualTo("image");
    }

    @Test
    void createsPopularResponseFromRedisDto() {
        KisPopularRedisDto redisDto = new KisPopularRedisDto("삼성전자", "005930", "1", "70000", "2", "1000", "1.45", "image");

        PopularStockResponseDto result = PopularStockResponseDto.from(redisDto);

        assertThat(result.getRank()).isEqualTo("1");
        assertThat(result.getStockCode()).isEqualTo("005930");
    }

    @Test
    void createsPeriodResponseFromCalculatedResult() {
        StockPeriodResult result = new StockPeriodResult(
                "005930",
                LocalDate.of(2025, 1, 1),
                "D",
                "69000",
                "71000",
                "68000",
                "70000",
                "1000",
                "70000000",
                69500,
                -500,
                500,
                1500,
                -1500
        );

        StockPeriodResponseDto response = StockPeriodResponseDto.from(result);

        assertThat(response.getPrevPrice()).isEqualTo(69500);
        assertThat(response.getCloseFromPrev()).isEqualTo(500);
    }

    private Stock stock() {
        return Stock.builder()
                .stockCode("005930")
                .name("삼성전자")
                .price("70000")
                .changeAmount("1000")
                .sign("2")
                .changeRate("1.45")
                .stockImage("image")
                .build();
    }
}
