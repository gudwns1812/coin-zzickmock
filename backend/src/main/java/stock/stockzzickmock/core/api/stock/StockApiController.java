package stock.stockzzickmock.core.api.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.stockzzickmock.core.api.stock.dto.request.StockCountRequestDto;
import stock.stockzzickmock.core.api.stock.dto.response.CategoryPageResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.SearchResponseDto;
import stock.stockzzickmock.core.application.stock.StockService;
import stock.stockzzickmock.support.error.StockErrorType;
import stock.stockzzickmock.support.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v2/stocks")
public class StockApiController {

    private final StockService stockService;

    @GetMapping("/category")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<String>> getCategories() {
        List<String> dto = stockService.getCategories();
        return ApiResponse.success(dto);
    }

    @GetMapping("/category/{categoryName}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CategoryPageResponseDto> getCategoryStock(@PathVariable String categoryName,
                                                                 @RequestParam(defaultValue = "1") int page) {
        CategoryPageResponseDto categoryPageResponseDto = stockService.getCategoryStocks(categoryName, page);
        return ApiResponse.success(categoryPageResponseDto);

    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> searchStocks(@RequestParam(required = false) String keyword) {
        log.info("searchStocks called with keyword: {}", keyword);
        List<SearchResponseDto> searchResponseDtos = stockService.searchStocks(keyword);
        if (keyword == null || keyword.isEmpty()) {
            return ApiResponse.error(searchResponseDtos, StockErrorType.EMPTY_SEARCH_KEYWORD);
        }
        return ApiResponse.success(searchResponseDtos);
    }

    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> stockCounter(@RequestBody StockCountRequestDto dto) {
        stockService.stockSearchCounter(dto.getStockCode());
        return ApiResponse.success();
    }
}
