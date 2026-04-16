package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {
    List<PositionSnapshot> findOpenPositions(String memberId);

    Optional<PositionSnapshot> findOpenPosition(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode
    );

    PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot);

    void delete(String memberId, String symbol, String positionSide, String marginMode);
}
