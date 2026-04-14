package coin.coinzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import coin.coinzzickmock.core.domain.stock.Stock;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;

@Component
@RequiredArgsConstructor
public class StockSearchHandler {

    private static final int SEARCH_LIMIT = 5;
    private final StockJpaRepository stockJpaRepository;

    public List<Stock> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return popularStocks();
        }

        List<Stock> stocks;
        if (keyword.matches("\\d+")) {
            stocks = stockJpaRepository.searchByStockCode(keyword, PageRequest.of(0, SEARCH_LIMIT))
                    .stream()
                    .map(StockEntity::toDomain)
                    .toList();
        } else {
            stocks = stockJpaRepository.searchByName(keyword, PageRequest.of(0, SEARCH_LIMIT))
                    .stream()
                    .map(StockEntity::toDomain)
                    .toList();
        }

        if (stocks.isEmpty()) {
            return popularStocks();
        }

        return stocks;
    }

    private List<Stock> popularStocks() {
        return stockJpaRepository.findTop6ByOrderByStockSearchCountDesc().stream()
                .limit(SEARCH_LIMIT)
                .map(StockEntity::toDomain)
                .toList();
    }
}
