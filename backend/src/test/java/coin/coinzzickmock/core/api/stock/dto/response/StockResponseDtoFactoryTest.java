package coin.coinzzickmock.core.api.stock.dto.response;

import org.junit.jupiter.api.Test;
import coin.coinzzickmock.core.application.stock.result.CategoryPageResult;
import coin.coinzzickmock.core.application.stock.result.MarketIndexResult;
import coin.coinzzickmock.core.application.stock.result.MarketIndicesResult;
import coin.coinzzickmock.core.application.stock.result.PopularStockResult;
import coin.coinzzickmock.core.application.stock.result.StockInfoResult;
import coin.coinzzickmock.core.application.stock.result.StockPeriodResult;
import coin.coinzzickmock.core.application.stock.result.StockSummaryResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockResponseDtoFactoryTest {

    @Test
    void createsSearchResponseFromStock() {
        StockSummaryResult stock = stockSummary();

        SearchResponseDto result = SearchResponseDto.from(stock);

        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getStockName()).isEqualTo("삼성전자");
    }

    @Test
    void createsCategoryResponseFromStock() {
        StockSummaryResult stock = stockSummary();

        CategoryStockResponseDto result = CategoryStockResponseDto.from(stock);

        assertThat(result.getCurrentPrice()).isEqualTo("70000");
        assertThat(result.getStockImage()).isEqualTo("image");
    }

    @Test
    void createsPopularResponseFromRedisDto() {
        PopularStockResult redisDto = new PopularStockResult(
                "삼성전자",
                "005930",
                "1",
                "70000",
                "2",
                "1000",
                "1.45",
                "image"
        );

        PopularStockResponseDto result = PopularStockResponseDto.from(redisDto);

        assertThat(result.getRank()).isEqualTo("1");
        assertThat(result.getStockCode()).isEqualTo("005930");
    }

    @Test
    void createsStockInfoResponseFromStock() {
        StockInfoResult stock = new StockInfoResult(
                "005930",
                "삼성전자",
                null,
                "70000",
                null,
                null,
                null,
                null,
                "1000",
                "2",
                "1.45",
                null,
                null,
                "image"
        );

        StockInfoResponseDto result = StockInfoResponseDto.from(stock);

        assertThat(result.getStockName()).isEqualTo("삼성전자");
        assertThat(result.getCategoryName()).isNull();
    }

    @Test
    void createsIndicesResponseFromDomain() {
        MarketIndicesResult indices = new MarketIndicesResult(
                "10",
                "2",
                "1.2",
                List.of(new MarketIndexResult(
                        "20250101",
                        "300.1",
                        "301.0",
                        "299.9",
                        "1000",
                        "2000"
                ))
        );

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

    @Test
    void createsCategoryPageResponseFromApplicationResult() {
        CategoryPageResult page = new CategoryPageResult(3, List.of(stockSummary()));

        CategoryPageResponseDto result = CategoryPageResponseDto.from(page);

        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getStocks()).hasSize(1);
    }

    private StockSummaryResult stockSummary() {
        return new StockSummaryResult(
                "삼성전자",
                "005930",
                "70000",
                "2",
                "1000",
                "1.45",
                "image"
        );
    }
}
