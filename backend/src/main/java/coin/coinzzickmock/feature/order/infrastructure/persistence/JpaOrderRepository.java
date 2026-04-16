package coin.coinzzickmock.feature.order.infrastructure.persistence;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class JpaOrderRepository implements OrderRepository {
    private final FuturesOrderSpringDataRepository futuresOrderSpringDataRepository;

    public JpaOrderRepository(FuturesOrderSpringDataRepository futuresOrderSpringDataRepository) {
        this.futuresOrderSpringDataRepository = futuresOrderSpringDataRepository;
    }

    @Override
    @Transactional
    public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
        return futuresOrderSpringDataRepository.save(FuturesOrderJpaEntity.from(memberId, futuresOrder)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesOrder> findByMemberId(String memberId) {
        return futuresOrderSpringDataRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FuturesOrderJpaEntity::toDomain)
                .toList();
    }
}
