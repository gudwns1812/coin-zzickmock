package stock.stockzzickmock.core.application.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.stockzzickmock.core.api.stock.dto.response.CategoryPageResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.SearchResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import stock.stockzzickmock.core.application.stock.implement.StockCommandHandler;
import stock.stockzzickmock.core.application.stock.implement.StockLoader;
import stock.stockzzickmock.core.application.stock.implement.StockPeriodCalculator;
import stock.stockzzickmock.core.application.stock.implement.StockSearchHandler;
import stock.stockzzickmock.core.domain.stock.Stock;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockLoader stockLoader;
    private final StockCommandHandler stockCommandHandler;
    private final StockPeriodCalculator stockPeriodCalculator;
    private final StockSearchHandler stockSearchHandler;

    public Stock getStockInfo(String stockCode) {
        return stockLoader.loadInfo(stockCode);
    }

    public List<StockPeriodResponseDto> getStockPeriodInfo(String stockCode, String type) {
        return stockLoader.loadHistory(stockCode, type).stream()
                .map(stockPeriodCalculator::calculate)
                .map(StockPeriodResponseDto::from)
                .toList();
    }

    public void recordSearchSelection(String stockCode) {
        stockCommandHandler.increaseSearchCount(stockCode);
    }

    public List<String> getCategories() {
        return stockLoader.loadCategories();
    }

    public CategoryPageResponseDto getCategoryStocks(String category, int page) {
        return CategoryPageResponseDto.of(stockLoader.loadCategoryPage(category, page));
    }

    public List<SearchResponseDto> searchStocks(String keyword) {
        return stockSearchHandler.search(keyword).stream()
                .map(SearchResponseDto::from)
                .toList();
    }

    public void publishActiveStockSet(String source, List<String> stockCodes) {
        stockCommandHandler.publishActiveStockSet(source, stockCodes);
    }
}
