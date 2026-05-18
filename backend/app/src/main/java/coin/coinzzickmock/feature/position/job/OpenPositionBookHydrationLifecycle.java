package coin.coinzzickmock.feature.position.job;

import coin.coinzzickmock.feature.position.application.implement.OpenPositionBookHydrator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenPositionBookHydrationLifecycle implements SmartLifecycle {
    public static final int PHASE = Integer.MIN_VALUE + 110;

    private final OpenPositionBookHydrator openPositionBookHydrator;
    private volatile boolean running;

    @Override
    public void start() {
        openPositionBookHydrator.hydrate();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
