package stock.stockzzickmock.core.application.stock.implement;

import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.application.stock.implement.result.CategoryStocksPage;
import stock.stockzzickmock.core.domain.stock.Stock;

import java.util.Comparator;
import java.util.List;

@Component
public class CategoryStockSorter {

    private static final int PAGE_SIZE = 10;

    public CategoryStocksPage sortAndSlice(List<Stock> stocks, int page) {
        List<Stock> sortedStocks = stocks.stream()
                .sorted(Comparator.comparingLong(this::volumeValue).reversed())
                .toList();

        int totalPages = Math.max(1, (sortedStocks.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int fromIndex = Math.max(0, (page - 1) * PAGE_SIZE);
        if (fromIndex >= sortedStocks.size()) {
            return new CategoryStocksPage(totalPages, List.of());
        }

        int toIndex = Math.min(fromIndex + PAGE_SIZE, sortedStocks.size());
        return new CategoryStocksPage(totalPages, sortedStocks.subList(fromIndex, toIndex));
    }

    private long volumeValue(Stock stock) {
        try {
            return Long.parseLong(stock.getVolumeValue());
        } catch (NumberFormatException | NullPointerException exception) {
            return 0L;
        }
    }
}
