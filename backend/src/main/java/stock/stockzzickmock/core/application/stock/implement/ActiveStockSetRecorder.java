package stock.stockzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.storage.redis.stock.publisher.StockActiveSetPublisher;

@Component
@RequiredArgsConstructor
public class ActiveStockSetRecorder {

    private final StockActiveSetPublisher stockActiveSetPublisher;

    public void record(String source, List<String> stockCodes) {
        stockActiveSetPublisher.publish(source, stockCodes);
    }
}
