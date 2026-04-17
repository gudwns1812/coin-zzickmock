package coin.coinzzickmock.feature.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuturesOrderEntityRepository extends JpaRepository<FuturesOrderEntity, Long> {
    List<FuturesOrderEntity> findAllByMemberIdOrderByCreatedAtDesc(String memberId);

    void deleteAllByMemberId(String memberId);
}
