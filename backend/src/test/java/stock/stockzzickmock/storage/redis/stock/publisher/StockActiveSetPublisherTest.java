package stock.stockzzickmock.storage.redis.stock.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.StockErrorType;

@ExtendWith(MockitoExtension.class)
class StockActiveSetPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private StockActiveSetPublisher stockActiveSetPublisher;

    @BeforeEach
    void setUp() {
        stockActiveSetPublisher = new StockActiveSetPublisher(
                redisTemplate,
                "stock:collector:active-sets"
        );
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    void publishesSourceAndStockCodesToActiveSetStream() {
        when(streamOperations.add(any(MapRecord.class))).thenReturn(RecordId.of("1-0"));

        stockActiveSetPublisher.publish("portfolio", List.of("005930", "000660"));

        ArgumentCaptor<MapRecord<String, Object, Object>> recordCaptor =
                ArgumentCaptor.forClass((Class) MapRecord.class);
        verify(streamOperations).add(recordCaptor.capture());
        MapRecord<String, Object, Object> record = recordCaptor.getValue();
        assertThat(record.getStream()).isEqualTo("stock:collector:active-sets");
        assertThat(record.getValue()).containsEntry("source", "portfolio");
        assertThat(record.getValue()).containsEntry("stockCodes", List.of("005930", "000660"));
    }

    @Test
    void throwsCoreExceptionWhenRedisStreamPublishFails() {
        when(streamOperations.add(any(MapRecord.class))).thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> stockActiveSetPublisher.publish("portfolio", List.of("005930")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(StockErrorType.STOCK_ACTIVE_SET_PUBLISH_FAILED);
    }
}
