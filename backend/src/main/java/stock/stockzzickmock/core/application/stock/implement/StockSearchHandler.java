package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockSearchHandler {

    private static final int SEARCH_LIMIT = 5;
    private static final int POPULAR_SEARCH_LIMIT = 6;

    private final StockRepository stockRepository;

    public List<Stock> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return popularStocks();
        }

        List<Stock> stocks = keyword.matches("\\d+")
                ? stockRepository.searchByStockCode(keyword, PageRequest.of(0, SEARCH_LIMIT))
                : stockRepository.searchByName(keyword, PageRequest.of(0, SEARCH_LIMIT));

        if (stocks.isEmpty()) {
            return popularStocks();
        }

        return stocks;
    }

    private List<Stock> popularStocks() {
        return stockRepository.findTop6ByOrderByStockSearchCountDesc().stream()
                .limit(SEARCH_LIMIT)
                .toList();
    }
}
