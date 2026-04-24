package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
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

    List<OpenPositionCandidate> findOpenBySymbol(String symbol);

    PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot);

    boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode);

    void delete(String memberId, String symbol, String positionSide, String marginMode);
}
