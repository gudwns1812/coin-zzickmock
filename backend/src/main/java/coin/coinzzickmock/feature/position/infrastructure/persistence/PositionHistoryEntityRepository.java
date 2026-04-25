package coin.coinzzickmock.feature.position.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionHistoryEntityRepository extends JpaRepository<PositionHistoryEntity, Long> {
    List<PositionHistoryEntity> findAllByMemberIdOrderByClosedAtDesc(String memberId);

    List<PositionHistoryEntity> findAllByMemberIdAndSymbolOrderByClosedAtDesc(String memberId, String symbol);
}
