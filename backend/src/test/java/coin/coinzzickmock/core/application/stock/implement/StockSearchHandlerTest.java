package coin.coinzzickmock.core.application.stock.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import coin.coinzzickmock.core.domain.stock.Stock;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;

@ExtendWith(MockitoExtension.class)
class StockSearchHandlerTest {

    @Mock
    private StockJpaRepository stockJpaRepository;

    @InjectMocks
    private StockSearchHandler stockSearchHandler;

    @Test
    void returnsPopularStocksWhenKeywordIsBlank() {
        when(stockJpaRepository.findTop6ByOrderByStockSearchCountDesc()).thenReturn(
                List.of(
                        entity("A1"), entity("A2"), entity("A3"),
                        entity("A4"), entity("A5")
                )
        );

        List<Stock> result = stockSearchHandler.search(" ");

        assertThat(result).hasSize(5);
        assertThat(result).extracting(Stock::getStockCode)
                .containsExactly("A1", "A2", "A3", "A4", "A5");
    }

    @Test
    void searchesByStockCodeWhenKeywordIsNumeric() {
        when(stockJpaRepository.searchByStockCode(any(), any())).thenReturn(
                List.of(entity("005930")));

        List<Stock> result = stockSearchHandler.search("005");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo("005930");
        verify(stockJpaRepository).searchByStockCode(any(), any());
    }

    @Test
    void fallsBackToPopularStocksWhenSearchResultIsEmpty() {
        when(stockJpaRepository.searchByName(any(), any())).thenReturn(List.of());
        when(stockJpaRepository.findTop6ByOrderByStockSearchCountDesc()).thenReturn(
                List.of(entity("A1"), entity("A2"), entity("A3"))
        );

        List<Stock> result = stockSearchHandler.search("없는종목");

        assertThat(result).extracting(Stock::getStockCode)
                .containsExactly("A1", "A2", "A3");
    }

    @Test
    void searchesByNameWhenKeywordIsNotNumeric() {
        when(stockJpaRepository.searchByName(any(), any())).thenReturn(
                List.of(entity("005930")));

        List<Stock> result = stockSearchHandler.search("삼성");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("종목-005930");
        verify(stockJpaRepository).searchByName(any(), any());
    }

    private StockEntity entity(String stockCode) {
        return StockEntity.builder()
                .stockCode(stockCode)
                .name("종목-" + stockCode)
                .build();
    }
}
