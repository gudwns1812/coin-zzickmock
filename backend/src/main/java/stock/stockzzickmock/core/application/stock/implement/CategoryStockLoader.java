package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryStockLoader {

    private final StockRepository stockRepository;

    public List<Stock> load(String category) {
        return stockRepository.findAllByCategory(category);
    }
}
