package coin.coinzzickmock.feature.position.application.repository;

import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {
    List<PositionSnapshot> findOpenPositions(Long memberId);

    default boolean existsOpenByMemberId(Long memberId) {
        return !findOpenPositions(memberId).isEmpty();
    }

    Optional<PositionSnapshot> findOpenPosition(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    );

    default Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide) {
        return findOpenPositions(memberId).stream()
                .filter(position -> position.symbol().equalsIgnoreCase(symbol))
                .filter(position -> position.positionSide().equalsIgnoreCase(positionSide))
                .findFirst();
    }

    List<OpenPositionCandidate> findOpenBySymbol(String symbol);

    PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot);

    default PositionMutationResult updateWithVersion(
            Long memberId,
            PositionSnapshot expectedPosition,
            PositionSnapshot nextPosition
    ) {
        Optional<PositionSnapshot> current = findOpenPosition(
                memberId,
                expectedPosition.symbol(),
                expectedPosition.positionSide(),
                expectedPosition.marginMode()
        );
        if (current.isEmpty()) {
            return PositionMutationResult.notFound();
        }
        if (current.orElseThrow().version() != expectedPosition.version()) {
            return PositionMutationResult.staleVersion(current.orElseThrow());
        }
        PositionSnapshot saved = save(memberId, nextPosition.withVersion(expectedPosition.version() + 1));
        return PositionMutationResult.updated(1, saved);
    }

    default PositionMutationResult deleteWithVersion(Long memberId, PositionSnapshot expectedPosition) {
        Optional<PositionSnapshot> current = findOpenPosition(
                memberId,
                expectedPosition.symbol(),
                expectedPosition.positionSide(),
                expectedPosition.marginMode()
        );
        if (current.isEmpty()) {
            return PositionMutationResult.notFound();
        }
        if (current.orElseThrow().version() != expectedPosition.version()) {
            return PositionMutationResult.staleVersion(current.orElseThrow());
        }
        return deleteIfOpen(memberId, expectedPosition.symbol(), expectedPosition.positionSide(), expectedPosition.marginMode())
                ? PositionMutationResult.deleted(1)
                : PositionMutationResult.notFound();
    }

    boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode);

    void delete(Long memberId, String symbol, String positionSide, String marginMode);
}
