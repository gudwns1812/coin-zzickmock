package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {
    List<PositionSnapshot> findOpenPositions(Long memberId);

    boolean existsOpenByMemberId(Long memberId);

    Optional<PositionSnapshot> findOpenPosition(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    );

    Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide);

    List<OpenPositionCandidate> findOpenBySymbol(String symbol);

    PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot);

    PositionMutationResult updateWithVersion(
            Long memberId,
            PositionSnapshot expectedPosition,
            PositionSnapshot nextPosition
    );

    PositionMutationResult deleteWithVersion(Long memberId, PositionSnapshot expectedPosition);

    boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode);

    void delete(Long memberId, String symbol, String positionSide, String marginMode);
}
