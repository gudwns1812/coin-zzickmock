package coin.coinzzickmock.storage.db.stock.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import coin.coinzzickmock.storage.db.stock.entity.StockHistoryEntity;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistoryEntity, Long> {

    List<StockHistoryEntity> findByStockCodeAndTypeOrderByDateAsc(String stockCode, String type);
}
