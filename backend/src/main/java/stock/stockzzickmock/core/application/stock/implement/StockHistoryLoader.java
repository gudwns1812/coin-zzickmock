package stock.stockzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.StockHistory;
import stock.stockzzickmock.storage.db.stock.entity.StockHistoryEntity;
import stock.stockzzickmock.storage.db.stock.repository.StockHistoryJpaRepository;

@Component
@RequiredArgsConstructor
public class StockHistoryLoader {

    private final StockHistoryJpaRepository stockHistoryJpaRepository;

    public List<StockHistory> load(String stockCode, String type) {
        return stockHistoryJpaRepository.findByStockCodeAndTypeOrderByDateAsc(stockCode, type).stream()
                .map(StockHistoryEntity::toDomain)
                .toList();
    }
}
