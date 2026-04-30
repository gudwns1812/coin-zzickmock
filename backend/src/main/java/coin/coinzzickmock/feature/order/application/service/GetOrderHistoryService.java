package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.OrderHistoryResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderHistoryService {
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderHistoryResult> getOrderHistory(Long memberId, String symbol) {
        return orderRepository.findByMemberId(memberId).stream()
                .filter(order -> matchesSymbol(order, symbol))
                .map(this::toResult)
                .toList();
    }

    private boolean matchesSymbol(FuturesOrder order, String symbol) {
        return symbol == null || symbol.isBlank() || order.symbol().equalsIgnoreCase(symbol);
    }

    private OrderHistoryResult toResult(FuturesOrder order) {
        return new OrderHistoryResult(
                order.orderId(),
                order.symbol(),
                order.positionSide(),
                order.orderType(),
                order.orderPurpose(),
                order.marginMode(),
                order.leverage(),
                order.quantity(),
                order.limitPrice(),
                order.status(),
                order.feeType(),
                order.estimatedFee(),
                order.executionPrice(),
                order.orderTime(),
                order.triggerPrice(),
                order.triggerType(),
                order.triggerSource(),
                order.ocoGroupId()
        );
    }
}
