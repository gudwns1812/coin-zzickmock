package coin.coinzzickmock.feature.order.infrastructure.persistence;

import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceRepository implements OrderRepository {
    private final FuturesOrderEntityRepository futuresOrderEntityRepository;

    @Override
    @Transactional
    public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
        return futuresOrderEntityRepository.save(FuturesOrderEntity.from(memberId, futuresOrder)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesOrder> findByMemberId(Long memberId) {
        return futuresOrderEntityRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FuturesOrderEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
        return futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .map(FuturesOrderEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
        return futuresOrderEntityRepository.findAllBySymbolAndStatusOrderByCreatedAtAsc(
                        symbol,
                        FuturesOrder.STATUS_PENDING
                ).stream()
                .map(entity -> new PendingOrderCandidate(entity.memberId(), entity.toDomain()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingOrderCandidate> findExecutablePendingLimitOrders(
            String symbol,
            double lowerPrice,
            double upperPrice,
            boolean sellSide
    ) {
        return futuresOrderEntityRepository.findExecutablePendingLimitOrders(
                        symbol,
                        FuturesOrder.STATUS_PENDING,
                        BigDecimal.valueOf(lowerPrice),
                        BigDecimal.valueOf(upperPrice),
                        sellSide,
                        FuturesOrder.PURPOSE_OPEN_POSITION,
                        FuturesOrder.PURPOSE_CLOSE_POSITION,
                        "LONG",
                        "SHORT"
                ).stream()
                .map(entity -> new PendingOrderCandidate(entity.memberId(), entity.toDomain()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesOrder> findPendingCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return futuresOrderEntityRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FuturesOrderEntity::toDomain)
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isClosePositionOrder)
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .filter(order -> order.marginMode().equalsIgnoreCase(marginMode))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesOrder> findPendingOpenOrders(Long memberId, String symbol, String positionSide) {
        return futuresOrderEntityRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FuturesOrderEntity::toDomain)
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isOpenPositionOrder)
                .filter(order -> !order.isConditionalOrder())
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .toList();
    }

    @Override
    @Transactional
    public Optional<FuturesOrder> claimPendingFill(
            Long memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    ) {
        int updated = futuresOrderEntityRepository.markFilledIfPending(
                memberId,
                orderId,
                FuturesOrder.STATUS_PENDING,
                FuturesOrder.STATUS_FILLED,
                feeType,
                BigDecimal.valueOf(estimatedFee),
                BigDecimal.valueOf(executionPrice)
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .map(FuturesOrderEntity::toDomain);
    }

    @Override
    @Transactional
    public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
        FuturesOrderEntity entity = futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow();
        entity.updateStatus(status);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public boolean cancelPending(Long memberId, String orderId) {
        return futuresOrderEntityRepository.cancelIfPending(
                memberId,
                orderId,
                FuturesOrder.STATUS_PENDING,
                FuturesOrder.STATUS_CANCELLED
        ) > 0;
    }

    @Override
    @Transactional
    public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
        FuturesOrderEntity entity = futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow();
        entity.updateQuantityAndStatus(quantity, status);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public int cancelPendingOrders(Long memberId, List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return 0;
        }
        return futuresOrderEntityRepository.cancelAllPendingByOrderIdIn(
                memberId,
                orderIds,
                FuturesOrder.STATUS_PENDING,
                FuturesOrder.STATUS_CANCELLED
        );
    }

    @Override
    @Transactional
    public int capPendingOrderQuantity(Long memberId, List<String> orderIds, double maxQuantity) {
        if (orderIds.isEmpty()) {
            return 0;
        }
        return futuresOrderEntityRepository.capAllPendingQuantityByOrderIdIn(
                memberId,
                orderIds,
                BigDecimal.valueOf(maxQuantity),
                FuturesOrder.STATUS_PENDING
        );
    }
}
