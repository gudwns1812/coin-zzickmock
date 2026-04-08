package stock.stockzzickmock.storage.db.stock.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import stock.stockzzickmock.storage.db.stock.entity.StockHistoryEntity;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistoryEntity, Long> {

    List<StockHistoryEntity> findByStockCodeAndTypeOrderByDateAsc(String stockCode, String type);
}
