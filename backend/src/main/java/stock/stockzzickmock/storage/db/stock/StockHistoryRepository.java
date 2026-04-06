package stock.stockzzickmock.storage.db.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import stock.stockzzickmock.core.domain.stock.StockHistory;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByStockCodeAndTypeOrderByDateAsc(String stockCode, String type);
}
