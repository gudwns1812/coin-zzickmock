package stock.stockzzickmock.core.application.stock.implement;

import org.junit.jupiter.api.Test;
import stock.stockzzickmock.core.application.stock.implement.result.CategoryStocksPage;
import stock.stockzzickmock.core.domain.stock.Stock;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryStockSorterTest {

    private final CategoryStockSorter categoryStockSorter = new CategoryStockSorter();

    @Test
    void sortsByVolumeValueDescendingAndSlicesRequestedPage() {
        List<Stock> stocks = IntStream.rangeClosed(1, 12)
                .mapToObj(index -> Stock.builder()
                        .stockCode("CODE-" + index)
                        .volumeValue(String.valueOf(index * 100L))
                        .build())
                .toList();

        CategoryStocksPage page = categoryStockSorter.sortAndSlice(stocks, 2);

        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.stocks()).hasSize(2);
        assertThat(page.stocks()).extracting(Stock::getStockCode)
                .containsExactly("CODE-2", "CODE-1");
    }

    @Test
    void returnsEmptyStocksWhenPageIsOutOfRange() {
        CategoryStocksPage page = categoryStockSorter.sortAndSlice(
                List.of(Stock.builder().stockCode("005930").volumeValue("100").build()),
                3
        );

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.stocks()).isEmpty();
    }
}
