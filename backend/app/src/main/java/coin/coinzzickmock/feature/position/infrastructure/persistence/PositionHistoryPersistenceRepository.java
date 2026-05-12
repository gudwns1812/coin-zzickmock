package coin.coinzzickmock.feature.position.infrastructure.persistence;

import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PositionHistoryPersistenceRepository implements PositionHistoryRepository {
    private final PositionHistoryEntityRepository positionHistoryEntityRepository;

    @Override
    @Transactional
    public PositionHistory save(Long memberId, PositionHistory positionHistory) {
        return positionHistoryEntityRepository.save(PositionHistoryEntity.from(memberId, positionHistory)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionHistory> findByMemberId(Long memberId, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return positionHistoryEntityRepository.findAllByMemberIdOrderByClosedAtDesc(memberId).stream()
                    .map(PositionHistoryEntity::toDomain)
                    .toList();
        }
        return positionHistoryEntityRepository.findAllByMemberIdAndSymbolOrderByClosedAtDesc(memberId, symbol).stream()
                .map(PositionHistoryEntity::toDomain)
                .toList();
    }
}
