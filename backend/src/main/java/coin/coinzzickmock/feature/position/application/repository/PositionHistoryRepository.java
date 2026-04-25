package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.domain.PositionHistory;
import java.util.List;

public interface PositionHistoryRepository {
    PositionHistory save(String memberId, PositionHistory positionHistory);

    List<PositionHistory> findByMemberId(String memberId, String symbol);
}
