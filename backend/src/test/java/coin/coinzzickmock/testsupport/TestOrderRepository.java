package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import java.util.Optional;

public abstract class TestOrderRepository implements OrderRepository {
    @Override
    public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
        throw new UnsupportedOperationException("save is not implemented for this test fake");
    }

    @Override
    public List<FuturesOrder> findByMemberId(Long memberId) {
        return List.of();
    }

    @Override
    public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
        return Optional.empty();
    }

    @Override
    public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
        return List.of();
    }

    @Override
    public boolean existsPendingByMemberId(Long memberId) {
        return findByMemberId(memberId).stream().anyMatch(FuturesOrder::isPending);
    }

    @Override
    public List<PendingOrderCandidate> findExecutablePendingLimitOrders(
            String symbol,
            double lowerPrice,
            double upperPrice,
            boolean sellSide
    ) {
        return findPendingBySymbol(symbol).stream()
                .filter(candidate -> !candidate.order().isConditionalOrder())
                .filter(candidate -> candidate.order().limitPrice() != null)
                .filter(candidate -> candidate.order().limitPrice() >= lowerPrice)
                .filter(candidate -> candidate.order().limitPrice() <= upperPrice)
                .filter(candidate -> sellSide
                        ? candidate.order().isSellSideLimitOrder()
                        : candidate.order().isBuySideLimitOrder())
                .toList();
    }

    @Override
    public List<FuturesOrder> findPendingCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isClosePositionOrder)
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .filter(order -> order.marginMode().equalsIgnoreCase(marginMode))
                .toList();
    }

    @Override
    public List<FuturesOrder> findPendingOpenOrders(Long memberId, String symbol, String positionSide) {
        return findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isOpenPositionOrder)
                .filter(order -> !order.isConditionalOrder())
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .toList();
    }

    @Override
    public List<FuturesOrder> findPendingConditionalCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return findPendingCloseOrders(memberId, symbol, positionSide, marginMode).stream()
                .filter(FuturesOrder::isConditionalCloseOrder)
                .toList();
    }

    @Override
    public List<FuturesOrder> findPendingConditionalCloseOrdersBySymbol(String symbol) {
        return findPendingBySymbol(symbol).stream()
                .map(PendingOrderCandidate::order)
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isConditionalCloseOrder)
                .toList();
    }

    @Override
    public Optional<FuturesOrder> claimPendingFill(
            Long memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    ) {
        throw new UnsupportedOperationException("claimPendingFill is not implemented for this test fake");
    }

    @Override
    public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
        throw new UnsupportedOperationException("updateStatus is not implemented for this test fake");
    }

    @Override
    public FuturesOrder updatePendingConditionalCloseOrder(
            Long memberId,
            String orderId,
            int leverage,
            double quantity,
            double triggerPrice,
            String ocoGroupId
    ) {
        throw new UnsupportedOperationException("updatePendingConditionalCloseOrder is not implemented for this test fake");
    }

    @Override
    public int cancelPendingOrders(Long memberId, List<String> orderIds) {
        return (int) orderIds.stream()
                .filter(orderId -> cancelPending(memberId, orderId))
                .count();
    }

    @Override
    public boolean cancelPending(Long memberId, String orderId) {
        FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElse(null);
        if (order == null || !order.isPending()) {
            return false;
        }
        updateStatus(memberId, orderId, FuturesOrder.STATUS_CANCELLED);
        return true;
    }

    @Override
    public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
        if (FuturesOrder.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return updateStatus(memberId, orderId, status);
        }
        throw new UnsupportedOperationException("updateQuantityAndStatus is not implemented for this test fake");
    }

    @Override
    public int capPendingOrderQuantity(Long memberId, List<String> orderIds, double maxQuantity) {
        int updated = 0;
        for (String orderId : orderIds) {
            Optional<FuturesOrder> order = findByMemberIdAndOrderId(memberId, orderId)
                    .filter(FuturesOrder::isPending);
            if (order.isPresent()) {
                FuturesOrder current = order.orElseThrow();
                updateQuantityAndStatus(
                        memberId,
                        orderId,
                        Math.min(current.quantity(), maxQuantity),
                        FuturesOrder.STATUS_PENDING
                );
                updated++;
            }
        }
        return updated;
    }
}
