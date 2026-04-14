package coin.coinzzickmock.core.api.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import coin.coinzzickmock.core.api.stock.dto.response.IndicesResponseDto;
import coin.coinzzickmock.core.api.stock.dto.response.PopularStockResponseDto;
import coin.coinzzickmock.core.api.stock.dto.response.StockInfoResponseDto;
import coin.coinzzickmock.core.api.stock.dto.response.StockPeriodResponseDto;
import coin.coinzzickmock.core.application.stock.IndicesService;
import coin.coinzzickmock.core.application.stock.PopularService;
import coin.coinzzickmock.core.application.stock.StockService;
import coin.coinzzickmock.support.response.ApiResponse;

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
        List<StockPeriodResponseDto> response = stockService.getStockPeriodInfo(stockCode, period).stream()
                .map(StockPeriodResponseDto::from)
                .toList();
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
