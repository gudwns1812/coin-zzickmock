package stock.stockzzickmock.storage.db.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import stock.stockzzickmock.core.domain.stock.Stock;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long>{
    Optional<Stock> findByStockCode(String stockCode);

    @Query("SELECT DISTINCT s.category FROM Stock s order by s.category")
    List<String> findCategoryAll();

    @Query("SELECT s.stockCode FROM Stock s where s.category = :category")
    List<String> findAllStockCodesByCategory(String category);

    Integer countByCategory(String category);

    List<Stock> findAllByCategory(String category);

    List<Stock> findTop6ByOrderByStockSearchCountDesc();

    @Query("SELECT s FROM Stock s WHERE s.stockCode LIKE CONCAT('%', :query, '%') ORDER BY s.stockSearchCount DESC")
    List<Stock> searchByStockCode(String query, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE UPPER(s.name) LIKE CONCAT('%', UPPER(:query), '%') ORDER BY s.stockSearchCount DESC")
    List<Stock> searchByName(String query, Pageable pageable);

//    @Query("SELECT s FROM Stock s WHERE s.category = :category")
//    Page<Stock> findStockCodeByCategory(@Param("category") String category, Pageable pageable);
}
