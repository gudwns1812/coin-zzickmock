package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingLimitOrderBookHydrator {
    private final OrderRepository orderRepository;
    private final PendingLimitOrderBook pendingLimitOrderBook;

    @EventListener(ContextRefreshedEvent.class)
    public void hydrate() {
        pendingLimitOrderBook.hydrate(orderRepository.findPendingNonConditionalLimitOrders());
    }
}
