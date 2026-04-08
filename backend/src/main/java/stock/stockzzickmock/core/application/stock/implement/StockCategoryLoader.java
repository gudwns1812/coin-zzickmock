package stock.stockzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.storage.db.stock.repository.StockJpaRepository;

@Component
@RequiredArgsConstructor
public class StockCategoryLoader {

    private final StockJpaRepository stockJpaRepository;

    public List<String> load() {
        return stockJpaRepository.findCategoryAll();
    }
}
