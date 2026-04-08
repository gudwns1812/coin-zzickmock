package stock.stockzzickmock.core.application.stock.implement;

import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stock.stockzzickmock.storage.redis.stock.publisher.StockActiveSetPublisher;

@ExtendWith(MockitoExtension.class)
class ActiveStockSetRecorderTest {

    @Mock
    private StockActiveSetPublisher stockActiveSetPublisher;

    @InjectMocks
    private ActiveStockSetRecorder activeStockSetRecorder;

    @Test
    void publishesActiveStockSetSnapshot() {
        activeStockSetRecorder.record("portfolio", List.of("005930", "000660"));

        verify(stockActiveSetPublisher).publish("portfolio", List.of("005930", "000660"));
    }
}
