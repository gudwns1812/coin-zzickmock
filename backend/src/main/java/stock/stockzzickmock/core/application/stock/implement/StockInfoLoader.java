package stock.stockzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;
import stock.stockzzickmock.storage.redis.dto.StockDto;
import stock.stockzzickmock.storage.db.stock.StockRepository;

@Component
@RequiredArgsConstructor
public class StockInfoLoader {

    private static final String STOCK_KEY_PREFIX = "STOCK:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StockRepository stockRepository;

    public StockDto load(String stockCode) {
        Object stockInfo = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + stockCode);
        if (stockInfo != null) {
            return objectMapper.convertValue(stockInfo, StockDto.class);
        }

        return stockRepository.findByStockCode(stockCode)
                .map(StockDto::from)
                .orElseThrow(() -> new CoreException(StockErrorType.STOCK_NOT_FOUND));
    }
}
