package stock.stockzzickmock.core.api.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.stockzzickmock.core.api.stock.dto.response.IndicesResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.PopularStockResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockInfoResponseDto;
import stock.stockzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import stock.stockzzickmock.core.application.stock.IndicesService;
import stock.stockzzickmock.core.application.stock.PopularService;
import stock.stockzzickmock.core.application.stock.StockService;
import stock.stockzzickmock.support.response.ApiResponse;

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
    public ApiResponse<IndicesResponseDto> getIndicesByMarket(@PathVariable String market) {
        log.info("getIndicesByMarket called with market: {}", market);
        return ApiResponse.success(
                IndicesResponseDto.from(indicesService.getIndicesInfo(market))
        );
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<PopularStockResponseDto>> getPopularStocks() {
        List<PopularStockResponseDto> response = popularService.getPopularTop6Stock().stream()
                .map(PopularStockResponseDto::from)
                .toList();
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
    public ApiResponse<StockInfoResponseDto> getStockPrice(@PathVariable String stockCode) {
        return ApiResponse.success(
                StockInfoResponseDto.from(stockService.getStockInfo(stockCode))
        );
    }
}
