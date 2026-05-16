package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPendingLimitOrderBookHydrator implements SmartLifecycle {
    public static final int PHASE = Integer.MIN_VALUE + 100;

    private final OrderRepository orderRepository;
    private final OrderPendingLimitOrderBook pendingLimitOrderBook;
    private volatile boolean running;

    @Override
    public void start() {
        hydrate();
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

    public void hydrate() {
        pendingLimitOrderBook.hydrate(orderRepository.findPendingNonConditionalLimitOrders());
    }
}
