package coin.coinzzickmock.feature.order.infrastructure.persistence;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceRepository implements OrderRepository {
    private final FuturesOrderEntityRepository futuresOrderEntityRepository;

    @Override
    @Transactional
    public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
        return futuresOrderEntityRepository.save(FuturesOrderEntity.from(memberId, futuresOrder)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuturesOrder> findByMemberId(String memberId) {
        return futuresOrderEntityRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FuturesOrderEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
        return futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .map(FuturesOrderEntity::toDomain);
    }

    @Override
    @Transactional
    public FuturesOrder updateStatus(String memberId, String orderId, String status) {
        FuturesOrderEntity entity = futuresOrderEntityRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow();
        entity.updateStatus(status);
        return futuresOrderEntityRepository.save(entity).toDomain();
    }
}
