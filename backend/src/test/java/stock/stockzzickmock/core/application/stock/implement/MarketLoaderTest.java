package stock.stockzzickmock.core.application.stock.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import stock.stockzzickmock.core.domain.market.MarketIndices;
import stock.stockzzickmock.core.domain.market.PopularStock;
import stock.stockzzickmock.storage.redis.dto.IndicesRedisDto;
import stock.stockzzickmock.storage.redis.dto.KisIndicesRedisDto;
import stock.stockzzickmock.storage.redis.dto.KisPopularRedisDto;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;

@ExtendWith(MockitoExtension.class)
class MarketLoaderTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private MarketLoader marketLoader;

    @BeforeEach
    void setUp() {
        marketLoader = new MarketLoader(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loadsIndicesFromRedis() {
        Object redisValue = new Object();
        when(valueOperations.get("INDICES_INFO:KOSPI")).thenReturn(redisValue);
        when(objectMapper.convertValue(redisValue, IndicesRedisDto.class))
                .thenReturn(new IndicesRedisDto(
                        "10",
                        "2",
                        "1.2",
                        List.of(new KisIndicesRedisDto("20250101", "300", "301", "299", "1000", "2000"))
                ));

        MarketIndices result = marketLoader.loadIndices("KOSPI");

        assertThat(result.getPrev()).isEqualTo("10");
        assertThat(result.getIndices()).hasSize(1);
    }

    @Test
    void throwsWhenIndicesMissingInRedis() {
        when(valueOperations.get("INDICES_INFO:KOSPI")).thenReturn(null);

        assertThatThrownBy(() -> marketLoader.loadIndices("KOSPI"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(StockErrorType.INDICES_NOT_FOUND);
    }

    @Test
    void loadsPopularStocksFromRedis() {
        Object redisValue = new Object();
        when(valueOperations.get("POPULAR")).thenReturn(redisValue);
        when(objectMapper.convertValue(eq(redisValue), any(TypeReference.class)))
                .thenReturn(List.of(
                        new KisPopularRedisDto("삼성전자", "005930", "1", "70000", "2", "1000", "1.45", "image")
                ));

        List<PopularStock> result = marketLoader.loadPopularTop6();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo("005930");
    }

    @Test
    void throwsWhenPopularStocksMissingInRedis() {
        when(valueOperations.get("POPULAR")).thenReturn(null);

        assertThatThrownBy(() -> marketLoader.loadPopularTop6())
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(StockErrorType.POPULAR_NOT_FOUND);
    }
}
