package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.domain.PositionHistory;
import java.util.List;

public interface PositionHistoryRepository {
    PositionHistory save(Long memberId, PositionHistory positionHistory);

    List<PositionHistory> findByMemberId(Long memberId, String symbol);
}
