package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.StockHistory;
import stock.stockzzickmock.storage.db.stock.StockHistoryRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockHistoryLoader {

    private final StockHistoryRepository stockHistoryRepository;

    public List<StockHistory> load(String stockCode, String type) {
        return stockHistoryRepository.findByStockCodeAndTypeOrderByDateAsc(stockCode, type);
    }
}
