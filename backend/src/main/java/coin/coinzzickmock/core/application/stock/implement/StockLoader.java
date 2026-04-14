package coin.coinzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import coin.coinzzickmock.core.application.stock.implement.result.CategoryStocksPage;
import coin.coinzzickmock.core.domain.stock.Stock;
import coin.coinzzickmock.core.domain.stock.StockHistory;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.entity.StockHistoryEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockHistoryJpaRepository;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;
import coin.coinzzickmock.storage.redis.dto.StockDto;
import coin.coinzzickmock.support.error.CoreException;
import coin.coinzzickmock.support.error.StockErrorType;

@Component
@RequiredArgsConstructor
public class StockLoader {

    private static final String STOCK_KEY_PREFIX = "STOCK:";
    private static final int CATEGORY_PAGE_SIZE = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StockJpaRepository stockJpaRepository;
    private final StockHistoryJpaRepository stockHistoryJpaRepository;

    public Stock loadInfo(String stockCode) {
        Object stockInfo = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + stockCode);
        if (stockInfo != null) {
            return objectMapper.convertValue(stockInfo, StockDto.class).toDomain();
        }

        return stockJpaRepository.findByStockCode(stockCode)
                .map(StockEntity::toDomain)
                .orElseThrow(() -> new CoreException(StockErrorType.STOCK_NOT_FOUND));
    }

    public List<StockHistory> loadHistory(String stockCode, String type) {
        return stockHistoryJpaRepository.findByStockCodeAndTypeOrderByDateAsc(stockCode, type).stream()
                .map(StockHistoryEntity::toDomain)
                .toList();
    }

    public List<String> loadCategories() {
        return stockJpaRepository.findCategoryAll();
    }

    public List<Stock> loadByCategory(String category) {
        return stockJpaRepository.findAllByCategory(category).stream()
                .map(StockEntity::toDomain)
                .toList();
    }

    public CategoryStocksPage loadCategoryPage(String category, int page) {
        return sortAndSlice(loadByCategory(category), page);
    }

    private CategoryStocksPage sortAndSlice(List<Stock> stocks, int page) {
        List<Stock> sortedStocks = stocks.stream()
                .sorted(Comparator.comparingLong(this::volumeValue).reversed())
                .toList();

        int totalPages = Math.max(1, (sortedStocks.size() + CATEGORY_PAGE_SIZE - 1) / CATEGORY_PAGE_SIZE);
        int fromIndex = Math.max(0, (page - 1) * CATEGORY_PAGE_SIZE);
        if (fromIndex >= sortedStocks.size()) {
            return new CategoryStocksPage(totalPages, List.of());
        }

        int toIndex = Math.min(fromIndex + CATEGORY_PAGE_SIZE, sortedStocks.size());
        return new CategoryStocksPage(totalPages, sortedStocks.subList(fromIndex, toIndex));
    }

    private long volumeValue(Stock stock) {
        try {
            return Long.parseLong(stock.getVolumeValue());
        } catch (NumberFormatException | NullPointerException exception) {
            return 0L;
        }
    }
}
