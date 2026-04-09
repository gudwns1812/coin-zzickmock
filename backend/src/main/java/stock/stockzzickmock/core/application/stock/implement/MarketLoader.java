package stock.stockzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.market.MarketIndices;
import stock.stockzzickmock.core.domain.market.PopularStock;
import stock.stockzzickmock.storage.redis.dto.IndicesRedisDto;
import stock.stockzzickmock.storage.redis.dto.KisPopularRedisDto;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;

@Component
@RequiredArgsConstructor
public class MarketLoader {

    private static final String INDICES_KEY_PREFIX = "INDICES_INFO:";
    private static final String POPULAR_KEY = "POPULAR";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketIndices loadIndices(String market) {
        Object indicesInfo = redisTemplate.opsForValue().get(INDICES_KEY_PREFIX + market);
        if (indicesInfo == null) {
            throw new CoreException(StockErrorType.INDICES_NOT_FOUND);
        }

        return objectMapper.convertValue(indicesInfo, IndicesRedisDto.class).toDomain();
    }

    public List<PopularStock> loadPopularTop6() {
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
