package coin.coinzzickmock.feature.order.infrastructure.persistence;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FuturesOrderEntityRepository extends JpaRepository<FuturesOrderEntity, Long> {
    List<FuturesOrderEntity> findAllByMemberIdOrderByCreatedAtDesc(String memberId);

    List<FuturesOrderEntity> findAllBySymbolAndStatusOrderByCreatedAtAsc(String symbol, String status);

    Optional<FuturesOrderEntity> findByMemberIdAndOrderId(String memberId, String orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FuturesOrderEntity order
               set order.status = :filledStatus,
                   order.feeType = :feeType,
                   order.estimatedFee = :estimatedFee,
                   order.executionPrice = :executionPrice
             where order.memberId = :memberId
               and order.orderId = :orderId
               and order.status = :pendingStatus
            """)
    int markFilledIfPending(
            @Param("memberId") String memberId,
            @Param("orderId") String orderId,
            @Param("pendingStatus") String pendingStatus,
            @Param("filledStatus") String filledStatus,
            @Param("feeType") String feeType,
            @Param("estimatedFee") BigDecimal estimatedFee,
            @Param("executionPrice") BigDecimal executionPrice
    );

    void deleteAllByMemberId(String memberId);
}
