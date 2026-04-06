package stock.stockzzickmock.core.application.stock.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.storage.db.stock.StockRepository;
import stock.stockzzickmock.storage.redis.dto.StockDto;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockInfoLoaderTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StockRepository stockRepository;

    private StockInfoLoader stockInfoLoader;

    @BeforeEach
    void setUp() {
        stockInfoLoader = new StockInfoLoader(redisTemplate, objectMapper, stockRepository);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsRedisStockInfoWhenPresent() {
        Object redisValue = Map.of("stockCode", "005930");
        StockDto stockDto = new StockDto("005930", "삼성전자", "전기·전자", "70000", null, null, null, "KOSPI", "1000", "2", "1.45", "1000", "90000000", "image");
        when(valueOperations.get("STOCK:005930")).thenReturn(redisValue);
        when(objectMapper.convertValue(redisValue, StockDto.class)).thenReturn(stockDto);

        StockDto result = stockInfoLoader.load("005930");

        assertThat(result.getStockCode()).isEqualTo("005930");
        verify(stockRepository, org.mockito.Mockito.never()).findByStockCode(anyString());
    }

    @Test
    void fallsBackToDatabaseWhenRedisMisses() {
        when(valueOperations.get("STOCK:005930")).thenReturn(null);
        when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(
                Stock.builder()
                        .stockCode("005930")
                        .name("삼성전자")
                        .category("전기·전자")
                        .price("70000")
                        .marketName("KOSPI")
                        .changeAmount("1000")
                        .sign("2")
                        .changeRate("1.45")
                        .volume("1000")
                        .volumeValue("90000000")
                        .stockImage("image")
                        .build()
        ));

        StockDto result = stockInfoLoader.load("005930");

        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getStockName()).isEqualTo("삼성전자");
    }

    @Test
    void throwsWhenStockDoesNotExistAnywhere() {
        when(valueOperations.get("STOCK:404")).thenReturn(null);
        when(stockRepository.findByStockCode("404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockInfoLoader.load("404"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(StockErrorType.STOCK_NOT_FOUND);
    }
}
