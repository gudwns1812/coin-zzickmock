package stock.stockzzickmock.storage.db.stock.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import stock.stockzzickmock.storage.db.stock.entity.StockEntity;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByStockCode(String stockCode);

    @Query("SELECT DISTINCT s.category FROM Stock s order by s.category")
    List<String> findCategoryAll();

    List<StockEntity> findAllByCategory(String category);

    List<StockEntity> findTop6ByOrderByStockSearchCountDesc();

    @Query("SELECT s FROM Stock s WHERE s.stockCode LIKE CONCAT('%', :query, '%') ORDER BY s.stockSearchCount DESC")
    List<StockEntity> searchByStockCode(String query, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE UPPER(s.name) LIKE CONCAT('%', UPPER(:query), '%') ORDER BY s.stockSearchCount DESC")
    List<StockEntity> searchByName(String query, Pageable pageable);
}
