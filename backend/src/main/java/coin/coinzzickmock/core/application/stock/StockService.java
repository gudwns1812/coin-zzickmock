package coin.coinzzickmock.core.application.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import coin.coinzzickmock.core.application.stock.implement.StockCommandHandler;
import coin.coinzzickmock.core.application.stock.implement.StockLoader;
import coin.coinzzickmock.core.application.stock.implement.StockPeriodCalculator;
import coin.coinzzickmock.core.application.stock.implement.StockSearchHandler;
import coin.coinzzickmock.core.application.stock.result.CategoryPageResult;
import coin.coinzzickmock.core.application.stock.result.StockInfoResult;
import coin.coinzzickmock.core.application.stock.result.StockPeriodResult;
import coin.coinzzickmock.core.application.stock.result.StockSummaryResult;
import coin.coinzzickmock.support.error.CoreException;
import coin.coinzzickmock.support.error.StockErrorType;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockLoader stockLoader;
    private final StockCommandHandler stockCommandHandler;
    private final StockPeriodCalculator stockPeriodCalculator;
    private final StockSearchHandler stockSearchHandler;

    public StockInfoResult getStockInfo(String stockCode) {
        return StockInfoResult.from(stockLoader.loadInfo(stockCode));
    }

    public List<StockPeriodResult> getStockPeriodInfo(String stockCode, String type) {
        return stockLoader.loadHistory(stockCode, type).stream()
                .map(stockPeriodCalculator::calculate)
                .toList();
    }

    public void recordSearchSelection(String stockCode) {
        stockCommandHandler.increaseSearchCount(stockCode);
    }

    public List<String> getCategories() {
        return stockLoader.loadCategories();
    }

    public CategoryPageResult getCategoryStocks(String category, int page) {
        var categoryPage = stockLoader.loadCategoryPage(category, page);
        return new CategoryPageResult(
                categoryPage.totalPages(),
                categoryPage.stocks().stream()
                        .map(StockSummaryResult::from)
                        .toList()
        );
    }

    public List<StockSummaryResult> searchStocks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new CoreException(StockErrorType.EMPTY_SEARCH_KEYWORD);
        }

        return stockSearchHandler.search(keyword).stream()
                .map(StockSummaryResult::from)
                .toList();
    }

    public void publishActiveStockSet(String source, List<String> stockCodes) {
        stockCommandHandler.publishActiveStockSet(source, stockCodes);
    }
}
