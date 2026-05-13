package coin.coinzzickmock.feature.order.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.web.TradingExecutionSseBroker;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;

class TradingExecutionStreamEventBridgeTest {
    @Test
    void listensForTradingExecutionEventsAndForwardsThemToSseBroker() throws NoSuchMethodException {
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        TradingExecutionStreamEventBridge bridge = new TradingExecutionStreamEventBridge(broker);
        TradingExecutionEvent event = event();

        bridge.onTradingExecution(event);

        verify(broker).onTradingExecution(event);
        Method listenerMethod = TradingExecutionStreamEventBridge.class.getMethod(
                "onTradingExecution",
                TradingExecutionEvent.class
        );
        assertThat(listenerMethod.isAnnotationPresent(EventListener.class)).isTrue();
    }

    @Test
    void swallowsBrokerFailureSoEventPublicationDoesNotFail() {
        TradingExecutionSseBroker broker = mock(TradingExecutionSseBroker.class);
        TradingExecutionEvent event = event();
        doThrow(new IllegalStateException("stream unavailable")).when(broker).onTradingExecution(event);
        TradingExecutionStreamEventBridge bridge = new TradingExecutionStreamEventBridge(broker);

        assertThatCode(() -> bridge.onTradingExecution(event)).doesNotThrowAnyException();
    }

    private TradingExecutionEvent event() {
        return TradingExecutionEvent.orderFilled(
                1L,
                "order-1",
                "BTCUSDT",
                "LONG",
                "CROSS",
                1,
                74000
        );
    }
}
