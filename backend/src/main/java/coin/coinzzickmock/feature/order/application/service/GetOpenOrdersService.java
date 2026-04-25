package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.OpenOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOpenOrdersService {
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OpenOrderResult> getOpenOrders(String memberId, String symbol) {
        return orderRepository.findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(order -> symbol == null || symbol.isBlank() || order.symbol().equalsIgnoreCase(symbol))
                .map(this::toResult)
                .toList();
    }

    private OpenOrderResult toResult(FuturesOrder order) {
        return new OpenOrderResult(
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
                order.orderTime()
        );
    }
}
