package stock.stockzzickmock.core.api.stock.dto.response;

import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.application.stock.implement.result.StockPeriodResult;
import stock.stockzzickmock.core.domain.market.MarketIndexSnapshot;
import stock.stockzzickmock.core.domain.market.MarketIndices;
import stock.stockzzickmock.core.domain.market.PopularStock;
import stock.stockzzickmock.core.domain.stock.Stock;

import java.time.LocalDate;
import java.util.List;

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
        PopularStock redisDto = PopularStock.builder()
                .stockName("삼성전자")
                .stockCode("005930")
                .rank("1")
                .price("70000")
                .sign("2")
                .changeAmount("1000")
                .changeRate("1.45")
                .stockImage("image")
                .build();

        PopularStockResponseDto result = PopularStockResponseDto.from(redisDto);

        assertThat(result.getRank()).isEqualTo("1");
        assertThat(result.getStockCode()).isEqualTo("005930");
    }

    @Test
    void createsStockInfoResponseFromStock() {
        Stock stock = stock();

        StockInfoResponseDto result = StockInfoResponseDto.from(stock);

        assertThat(result.getStockName()).isEqualTo("삼성전자");
        assertThat(result.getCategoryName()).isNull();
    }

    @Test
    void createsIndicesResponseFromDomain() {
        MarketIndices indices = MarketIndices.builder()
                .prev("10")
                .sign("2")
                .prevRate("1.2")
                .indices(List.of(
                        MarketIndexSnapshot.builder()
                                .date("20250101")
                                .currentPrice("300.1")
                                .highPrice("301.0")
                                .lowPrice("299.9")
                                .accumulatedVolume("1000")
                                .accumulatedVolumePrice("2000")
                                .build()
                ))
                .build();

        IndicesResponseDto result = IndicesResponseDto.from(indices);

        assertThat(result.getPrevRate()).isEqualTo("1.2");
        assertThat(result.getIndices()).hasSize(1);
        assertThat(result.getIndices().get(0).getCurPrice()).isEqualTo("300.1");
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
