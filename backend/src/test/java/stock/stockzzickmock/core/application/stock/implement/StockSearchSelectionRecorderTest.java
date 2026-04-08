package stock.stockzzickmock.core.application.stock.implement;

import static org.mockito.Mockito.inOrder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockSearchSelectionRecorderTest {

    @Mock
    private StockSearchCounter stockSearchCounter;

    @InjectMocks
    private StockSearchSelectionRecorder stockSearchSelectionRecorder;

    @Test
    void recordsSelectionByIncreasingCount() {
        stockSearchSelectionRecorder.record("005930");

        InOrder inOrder = inOrder(stockSearchCounter);
        inOrder.verify(stockSearchCounter).increase("005930");
    }
}
