package coin.coinzzickmock.common.web;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SseDeliveryExecutor {
    private final Executor executor;

    public SseDeliveryExecutor(@Qualifier("sseDeliveryTaskExecutor") Executor executor) {
        this.executor = executor;
    }

    public void execute(Runnable command) {
        executor.execute(command);
    }
}
