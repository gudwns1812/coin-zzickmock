package stock.stockzzickmock.storage.redis.stock.publisher;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;

@Component
public class StockActiveSetPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String streamKey;

    public StockActiveSetPublisher(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${redis.stream.stock-active-sets.key:stock:collector:active-sets}") String streamKey
    ) {
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    public void publish(String source, List<String> stockCodes) {
        try {
            MapRecord<String, Object, Object> record = MapRecord.create(
                    streamKey,
                    Map.of(
                            "source", source,
                            "stockCodes", stockCodes
                    )
            );
            redisTemplate.opsForStream().add(record);
        } catch (RuntimeException exception) {
            throw new CoreException(StockErrorType.STOCK_ACTIVE_SET_PUBLISH_FAILED, exception);
        }
    }
}
