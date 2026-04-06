package stock.stockzzickmock.core.api.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.stockzzickmock.storage.redis.dto.IndicesRedisDto;
import stock.stockzzickmock.storage.redis.dto.StockDto;
import stock.stockzzickmock.core.api.stock.dto.response.PopularStockResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import stock.stockzzickmock.core.application.stock.IndicesService;
import stock.stockzzickmock.core.application.stock.PopularService;
import stock.stockzzickmock.core.application.stock.StockService;
import stock.stockzzickmock.support.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v2/stocks")
public class StockRealTimeController {

    private final IndicesService indicesService;
    private final PopularService popularService;
    private final StockService stockService;

    @GetMapping("/indices/{market}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<IndicesRedisDto> getIndicesByMarket(@PathVariable String market) {
        log.info("getIndicesByMarket called with market: {}", market);
        IndicesRedisDto indicesInfo = indicesService.getIndicesInfo(market);
        return ApiResponse.success(indicesInfo);
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<PopularStockResponseDto>> getPopularStocks() {
        List<PopularStockResponseDto> response = popularService.getPopularTop6Stock();
        return ApiResponse.success(response);
    }

    @GetMapping("/{stockCode}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<StockPeriodResponseDto>> stockPrice(@PathVariable String stockCode,
                                                                @RequestParam String period) {
        List<StockPeriodResponseDto> response = stockService.getStockPeriodInfo(stockCode, period);
        return ApiResponse.success(response);
    }

    @GetMapping("/info/{stockCode}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StockDto> getStockPrice(@PathVariable String stockCode) {
        StockDto stockInfo = stockService.getStockInfo(stockCode);
        return ApiResponse.success(stockInfo);
    }
}
