package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;

@Component
@RequiredArgsConstructor
public class StockSaver {

    private final StockRepository stockRepository;

    public void saveIfAbsent(Stock stock) {
        if (stockRepository.findByStockCode(stock.getStockCode()).isPresent()) {
            return;
        }
        stockRepository.save(stock);
    }
}
