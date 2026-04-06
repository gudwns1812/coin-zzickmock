package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.storage.db.stock.StockRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockCategoryLoader {

    private final StockRepository stockRepository;

    public List<String> load() {
        return stockRepository.findCategoryAll();
    }
}
