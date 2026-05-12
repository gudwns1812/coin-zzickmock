package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class SseDeliveryExecutorTest {

    @Test
    void executesDelegatedCommand() {
        boolean[] executed = {false};
        SseDeliveryExecutor executor = new SseDeliveryExecutor(command -> {
            executed[0] = true;
            command.run();
        });

        executor.execute(() -> executed[0] = true);

        assertThat(executed[0]).isTrue();
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
