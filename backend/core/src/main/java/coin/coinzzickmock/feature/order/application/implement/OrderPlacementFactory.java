package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacementFactory {
    public OrderPlacementRequest openPosition(CreateOrderCommand command) {
        return new OrderPlacementRequest(
                FuturesOrder.PURPOSE_OPEN_POSITION,
                command.positionSide(),
                command.orderType(),
                command.marginMode(),
                command.limitPrice(),
                command.quantity(),
                command.leverage()
        );
    }
}
