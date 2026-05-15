package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.ModifyOrderCommand;
import coin.coinzzickmock.feature.order.application.implement.OrderEditFillHandler;
import coin.coinzzickmock.feature.order.application.implement.OrderEditPlanner;
import coin.coinzzickmock.feature.order.application.implement.OrderEditPlanner.EditPlan;
import coin.coinzzickmock.feature.order.application.implement.OrderMutationLock;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.realtime.PendingLimitOrderBook;
import coin.coinzzickmock.feature.order.application.result.ModifyOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModifyOrderService {
    private final OrderRepository orderRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderPlacementPolicy orderPlacementPolicy;
    private final OrderMutationLock orderMutationLock;
    private final OrderEditPlanner orderEditPlanner;
    private final OrderEditFillHandler orderEditFillHandler;
    private final PendingLimitOrderBook pendingLimitOrderBook;

    @Transactional
    public ModifyOrderResult modify(ModifyOrderCommand command) {
        orderMutationLock.lock(command.memberId());
        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(command.memberId(), command.orderId())
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        EditPlan editPlan = orderEditPlanner.plan(order, command.limitPrice());

        FuturesOrder updated = orderRepository.updatePendingLimitPrice(
                command.memberId(),
                order.orderId(),
                editPlan.limitPrice(),
                editPlan.feeType(),
                editPlan.estimatedFee(),
                editPlan.limitPrice()
        ).orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        FuturesOrder result = fillIfMarketable(command.memberId(), updated, editPlan);
        if (result.isPending()) {
            pendingLimitOrderBook.replaceAfterCommit(command.memberId(), result);
        } else {
            pendingLimitOrderBook.removeAfterCommit(command.memberId(), result.orderId());
        }

        return ModifyOrderResult.from(result);
    }

    private FuturesOrder fillIfMarketable(Long memberId, FuturesOrder updated, EditPlan editPlan) {
        MarketSnapshot latestMarket = realtimeMarketPriceReader.requireFreshMarket(updated.symbol());
        var latestDecision = orderPlacementPolicy.decide(editPlan.placementRequest(), latestMarket.lastPrice());
        if (!latestDecision.executable()) {
            return updated;
        }

        return orderEditFillHandler.fill(memberId, updated, latestMarket, latestDecision);
    }
}
