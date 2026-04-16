package coin.coinzzickmock.feature.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuturesOrderSpringDataRepository extends JpaRepository<FuturesOrderJpaEntity, Long> {
    List<FuturesOrderJpaEntity> findAllByMemberIdOrderByCreatedAtDesc(String memberId);
}
