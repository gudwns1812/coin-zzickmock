package stock.stockzzickmock.core.application.stock.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockSearchSelectionRecorder {

    private final StockSearchCounter stockSearchCounter;

    @Transactional
    public void record(String stockCode) {
        stockSearchCounter.increase(stockCode);
    }
}
