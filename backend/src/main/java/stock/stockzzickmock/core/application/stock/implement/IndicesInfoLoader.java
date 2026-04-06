package stock.stockzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;
import stock.stockzzickmock.storage.redis.dto.IndicesRedisDto;

@Component
@RequiredArgsConstructor
public class IndicesInfoLoader {

    private static final String INDICES_KEY_PREFIX = "INDICES_INFO:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public IndicesRedisDto load(String market) {
        Object indicesInfo = redisTemplate.opsForValue().get(INDICES_KEY_PREFIX + market);
        if (indicesInfo == null) {
            throw new CoreException(StockErrorType.INDICES_NOT_FOUND);
        }
        return objectMapper.convertValue(indicesInfo, IndicesRedisDto.class);
    }
}
