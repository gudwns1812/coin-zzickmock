package coin.coinzzickmock.feature.order.infrastructure.persistence;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FuturesOrderEntityRepository extends JpaRepository<FuturesOrderEntity, Long> {
    List<FuturesOrderEntity> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<FuturesOrderEntity> findAllBySymbolAndStatusOrderByCreatedAtAsc(String symbol, String status);

    boolean existsByMemberIdAndStatus(Long memberId, String status);

    @Query("""
            select order
              from FuturesOrderEntity order
             where order.symbol = :symbol
               and order.status = :pendingStatus
               and order.limitPrice is not null
               and order.limitPrice between :lowerPrice and :upperPrice
               and order.triggerPrice is null
               and order.triggerType is null
               and order.triggerSource is null
               and order.ocoGroupId is null
               and (
                    (:sellSide = true and (
                        (order.orderPurpose = :openPurpose and order.positionSide = :shortSide)
                        or (order.orderPurpose = :closePurpose and order.positionSide = :longSide)
                    ))
                    or (:sellSide = false and (
                        (order.orderPurpose = :openPurpose and order.positionSide = :longSide)
                        or (order.orderPurpose = :closePurpose and order.positionSide = :shortSide)
                    ))
               )
             order by order.createdAt asc
            """)
    List<FuturesOrderEntity> findExecutablePendingLimitOrders(
            @Param("symbol") String symbol,
            @Param("pendingStatus") String pendingStatus,
            @Param("lowerPrice") BigDecimal lowerPrice,
            @Param("upperPrice") BigDecimal upperPrice,
            @Param("sellSide") boolean sellSide,
            @Param("openPurpose") String openPurpose,
            @Param("closePurpose") String closePurpose,
            @Param("longSide") String longSide,
            @Param("shortSide") String shortSide
    );

    Optional<FuturesOrderEntity> findByMemberIdAndOrderId(Long memberId, String orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FuturesOrderEntity order
               set order.status = :filledStatus,
                   order.feeType = :feeType,
                   order.estimatedFee = :estimatedFee,
                   order.executionPrice = :executionPrice,
                   order.activeConditionalTriggerType = null
             where order.memberId = :memberId
               and order.orderId = :orderId
               and order.status = :pendingStatus
            """)
    int markFilledIfPending(
            @Param("memberId") Long memberId,
            @Param("orderId") String orderId,
            @Param("pendingStatus") String pendingStatus,
            @Param("filledStatus") String filledStatus,
            @Param("feeType") String feeType,
            @Param("estimatedFee") BigDecimal estimatedFee,
            @Param("executionPrice") BigDecimal executionPrice
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FuturesOrderEntity order
               set order.status = :cancelledStatus,
                   order.activeConditionalTriggerType = null
             where order.memberId = :memberId
               and order.orderId = :orderId
               and order.status = :pendingStatus
            """)
    int cancelIfPending(
            @Param("memberId") Long memberId,
            @Param("orderId") String orderId,
            @Param("pendingStatus") String pendingStatus,
            @Param("cancelledStatus") String cancelledStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FuturesOrderEntity order
               set order.status = :cancelledStatus,
                   order.activeConditionalTriggerType = null
             where order.memberId = :memberId
               and order.orderId in :orderIds
               and order.status = :pendingStatus
            """)
    int cancelAllPendingByOrderIdIn(
            @Param("memberId") Long memberId,
            @Param("orderIds") List<String> orderIds,
            @Param("pendingStatus") String pendingStatus,
            @Param("cancelledStatus") String cancelledStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FuturesOrderEntity order
               set order.quantity = case
                        when order.quantity > :maxQuantity then :maxQuantity
                        else order.quantity
                   end,
                   order.status = :pendingStatus
             where order.memberId = :memberId
               and order.orderId in :orderIds
               and order.status = :pendingStatus
            """)
    int capAllPendingQuantityByOrderIdIn(
            @Param("memberId") Long memberId,
            @Param("orderIds") List<String> orderIds,
            @Param("maxQuantity") BigDecimal maxQuantity,
            @Param("pendingStatus") String pendingStatus
    );

    void deleteAllByMemberId(Long memberId);
}
