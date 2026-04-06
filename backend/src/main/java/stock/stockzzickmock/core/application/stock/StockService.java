package stock.stockzzickmock.core.application.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.stockzzickmock.core.api.stock.dto.response.CategoryPageResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.SearchResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import stock.stockzzickmock.core.application.stock.implement.CategoryStockLoader;
import stock.stockzzickmock.core.application.stock.implement.CategoryStockSorter;
import stock.stockzzickmock.core.application.stock.implement.StockCategoryLoader;
import stock.stockzzickmock.core.application.stock.implement.StockHistoryLoader;
import stock.stockzzickmock.core.application.stock.implement.StockInfoLoader;
import stock.stockzzickmock.core.application.stock.implement.StockPeriodCalculator;
import stock.stockzzickmock.core.application.stock.implement.StockSaver;
import stock.stockzzickmock.core.application.stock.implement.StockSearchCounter;
import stock.stockzzickmock.core.application.stock.implement.StockSearchHandler;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.redis.dto.StockDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockInfoLoader stockInfoLoader;
    private final StockHistoryLoader stockHistoryLoader;
    private final StockPeriodCalculator stockPeriodCalculator;
    private final StockSearchCounter stockSearchCounter;
    private final StockSaver stockSaver;
    private final StockCategoryLoader stockCategoryLoader;
    private final CategoryStockLoader categoryStockLoader;
    private final CategoryStockSorter categoryStockSorter;
    private final StockSearchHandler stockSearchHandler;

    public StockDto getStockInfo(String stockCode) {
        return stockInfoLoader.load(stockCode);
    }

    public List<StockPeriodResponseDto> getStockPeriodInfo(String stockCode, String type) {
        return stockHistoryLoader.load(stockCode, type).stream()
                .map(stockPeriodCalculator::calculate)
                .map(StockPeriodResponseDto::from)
                .toList();
    }

    @Transactional
    public void stockSearchCounter(String stockCode) {
        stockSearchCounter.increase(stockCode);
    }

    @Transactional
    public void saveStockDB(Stock stock) {
        stockSaver.saveIfAbsent(stock);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return stockCategoryLoader.load();
    }

    @Transactional(readOnly = true)
    public CategoryPageResponseDto getCategoryStocks(String category, int page) {
        return CategoryPageResponseDto.of(
                categoryStockSorter.sortAndSlice(categoryStockLoader.load(category), page)
        );
    }

    @Transactional(readOnly = true)
    public List<SearchResponseDto> searchStocks(String keyword) {
        return stockSearchHandler.search(keyword).stream()
                .map(SearchResponseDto::from)
                .toList();
    }
}
