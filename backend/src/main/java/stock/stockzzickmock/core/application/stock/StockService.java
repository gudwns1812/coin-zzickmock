package stock.stockzzickmock.core.application.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.stockzzickmock.core.api.stock.dto.response.CategoryPageResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.SearchResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import stock.stockzzickmock.core.application.stock.implement.CategoryStockLoader;
import stock.stockzzickmock.core.application.stock.implement.CategoryStockSorter;
import stock.stockzzickmock.core.application.stock.implement.ActiveStockSetRecorder;
import stock.stockzzickmock.core.application.stock.implement.StockCategoryLoader;
import stock.stockzzickmock.core.application.stock.implement.StockHistoryLoader;
import stock.stockzzickmock.core.application.stock.implement.StockInfoLoader;
import stock.stockzzickmock.core.application.stock.implement.StockPeriodCalculator;
import stock.stockzzickmock.core.application.stock.implement.StockSearchHandler;
import stock.stockzzickmock.core.application.stock.implement.StockSearchSelectionRecorder;
import stock.stockzzickmock.core.domain.stock.Stock;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockInfoLoader stockInfoLoader;
    private final StockHistoryLoader stockHistoryLoader;
    private final StockPeriodCalculator stockPeriodCalculator;
    private final ActiveStockSetRecorder activeStockSetRecorder;
    private final StockSearchSelectionRecorder stockSearchSelectionRecorder;
    private final StockCategoryLoader stockCategoryLoader;
    private final CategoryStockLoader categoryStockLoader;
    private final CategoryStockSorter categoryStockSorter;
    private final StockSearchHandler stockSearchHandler;

    public Stock getStockInfo(String stockCode) {
        return stockInfoLoader.load(stockCode);
    }

    public List<StockPeriodResponseDto> getStockPeriodInfo(String stockCode, String type) {
        return stockHistoryLoader.load(stockCode, type).stream()
                .map(stockPeriodCalculator::calculate)
                .map(StockPeriodResponseDto::from)
                .toList();
    }

    public void recordSearchSelection(String stockCode) {
        stockSearchSelectionRecorder.record(stockCode);
    }

    public List<String> getCategories() {
        return stockCategoryLoader.load();
    }

    public CategoryPageResponseDto getCategoryStocks(String category, int page) {
        return CategoryPageResponseDto.of(
                categoryStockSorter.sortAndSlice(categoryStockLoader.load(category), page)
        );
    }

    public List<SearchResponseDto> searchStocks(String keyword) {
        return stockSearchHandler.search(keyword).stream()
                .map(SearchResponseDto::from)
                .toList();
    }

    public void publishActiveStockSet(String source, List<String> stockCodes) {
        activeStockSetRecorder.record(source, stockCodes);
    }
}
