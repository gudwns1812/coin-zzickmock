package stock.stockzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.market.PopularStock;
import stock.stockzzickmock.storage.redis.dto.KisPopularRedisDto;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;

@Component
@RequiredArgsConstructor
public class PopularStockLoader {

    private static final String POPULAR_KEY = "POPULAR";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<PopularStock> load() {
        Object popularStocks = redisTemplate.opsForValue().get(POPULAR_KEY);
        if (popularStocks == null) {
            throw new CoreException(StockErrorType.POPULAR_NOT_FOUND);
        }
        return objectMapper.convertValue(popularStocks, new TypeReference<List<KisPopularRedisDto>>() {
        }).stream()
                .map(KisPopularRedisDto::toDomain)
                .toList();
    }
}
