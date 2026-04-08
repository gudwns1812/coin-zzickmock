package stock.stockzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.entity.StockEntity;
import stock.stockzzickmock.storage.db.stock.repository.StockJpaRepository;

@Component
@RequiredArgsConstructor
public class CategoryStockLoader {

    private final StockJpaRepository stockJpaRepository;

    public List<Stock> load(String category) {
        return stockJpaRepository.findAllByCategory(category).stream()
                .map(StockEntity::toDomain)
                .toList();
    }
}
