package coin.coinzzickmock.core.api.stock;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import coin.coinzzickmock.core.api.stock.dto.request.ActiveStockSetRequestDto;
import coin.coinzzickmock.core.api.stock.dto.request.StockCountRequestDto;
import coin.coinzzickmock.core.api.stock.dto.response.CategoryPageResponseDto;
import coin.coinzzickmock.core.api.stock.dto.response.SearchResponseDto;
import coin.coinzzickmock.core.application.stock.StockService;
import coin.coinzzickmock.support.response.ApiResponse;

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
        CategoryPageResponseDto categoryPageResponseDto =
                CategoryPageResponseDto.from(stockService.getCategoryStocks(categoryName, page));
        return ApiResponse.success(categoryPageResponseDto);

    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<SearchResponseDto>> searchStocks(@RequestParam(required = false) String keyword) {
        log.info("searchStocks called with keyword: {}", keyword);
        List<SearchResponseDto> searchResponseDtos = stockService.searchStocks(keyword).stream()
                .map(SearchResponseDto::from)
                .toList();
        return ApiResponse.success(searchResponseDtos);
    }

    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> recordSearchSelection(@Valid @RequestBody StockCountRequestDto request) {
        stockService.recordSearchSelection(request.stockCode());
        return ApiResponse.success();
    }

    @PostMapping("/active-sets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> publishActiveStockSet(@Valid @RequestBody ActiveStockSetRequestDto request) {
        stockService.publishActiveStockSet(request.source(), request.stockCodes());
        return ApiResponse.success();
    }
}
