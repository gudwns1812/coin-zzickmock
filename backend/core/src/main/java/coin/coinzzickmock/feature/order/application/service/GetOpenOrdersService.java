package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.dto.OpenOrderResult;
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
    public List<OpenOrderResult> getOpenOrders(Long memberId, String symbol) {
        return orderRepository.findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(order -> symbol == null || symbol.isBlank() || order.symbol().equalsIgnoreCase(symbol))
                .map(OpenOrderResult::from)
                .toList();
    }
}
