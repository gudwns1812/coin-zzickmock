package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CancelOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public CancelOrderResult cancel(Long memberId, String orderId) {
        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));

        if (!order.isPending()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        FuturesOrder cancelledOrder = orderRepository.updateStatus(memberId, orderId, "CANCELLED");
        return new CancelOrderResult(
                cancelledOrder.orderId(),
                cancelledOrder.symbol(),
                cancelledOrder.status()
        );
    }
}
