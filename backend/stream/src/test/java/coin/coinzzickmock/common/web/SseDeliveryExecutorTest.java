package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class SseDeliveryExecutorTest {

    @Test
    void executesDelegatedCommand() {
        int[] callCount = {0};
        SseDeliveryExecutor executor = new SseDeliveryExecutor(command -> {
            callCount[0]++;
            command.run();
        });

        executor.execute(() -> callCount[0]++);

        assertThat(callCount[0]).isEqualTo(2);
    }

    @Test
    void propagatesRejectedExecutionExceptionToBrokerBoundary() {
        SseDeliveryExecutor executor = new SseDeliveryExecutor(command -> {
            throw new RejectedExecutionException("queue full");
        });

        Throwable thrown = catchThrowable(() -> executor.execute(() -> {
        }));

        assertThat(thrown).isInstanceOf(RejectedExecutionException.class);
    }
}
