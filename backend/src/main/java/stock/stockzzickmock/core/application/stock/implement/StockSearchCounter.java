package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;

@Component
@RequiredArgsConstructor
public class StockSearchCounter {

    private final StockRepository stockRepository;

    public void increase(String stockCode) {
        stockRepository.findByStockCode(stockCode)
                .ifPresent(Stock::incrementStockSearchCount);
    }
}
