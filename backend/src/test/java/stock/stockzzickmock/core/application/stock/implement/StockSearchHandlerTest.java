package stock.stockzzickmock.core.application.stock.implement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSearchHandlerTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockSearchHandler stockSearchHandler;

    @Test
    void returnsPopularStocksWhenKeywordIsBlank() {
        when(stockRepository.findTop6ByOrderByStockSearchCountDesc()).thenReturn(
                List.of(
                        stock("A1"), stock("A2"), stock("A3"),
                        stock("A4"), stock("A5"), stock("A6")
                )
        );

        List<Stock> result = stockSearchHandler.search(" ");

        assertThat(result).hasSize(5);
        assertThat(result).extracting(Stock::getStockCode)
                .containsExactly("A1", "A2", "A3", "A4", "A5");
    }

    @Test
    void searchesByStockCodeWhenKeywordIsNumeric() {
        when(stockRepository.searchByStockCode(any(), any())).thenReturn(List.of(stock("005930")));

        List<Stock> result = stockSearchHandler.search("005");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo("005930");
        verify(stockRepository).searchByStockCode(any(), any());
    }

    @Test
    void fallsBackToPopularStocksWhenSearchResultIsEmpty() {
        when(stockRepository.searchByName(any(), any())).thenReturn(List.of());
        when(stockRepository.findTop6ByOrderByStockSearchCountDesc()).thenReturn(
                List.of(stock("A1"), stock("A2"), stock("A3"))
        );

        List<Stock> result = stockSearchHandler.search("없는종목");

        assertThat(result).extracting(Stock::getStockCode)
                .containsExactly("A1", "A2", "A3");
    }

    private Stock stock(String stockCode) {
        return Stock.builder()
                .stockCode(stockCode)
                .name("종목-" + stockCode)
                .build();
    }
}
