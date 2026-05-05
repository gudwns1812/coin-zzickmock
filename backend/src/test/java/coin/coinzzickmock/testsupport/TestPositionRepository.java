package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import java.util.Optional;

public abstract class TestPositionRepository implements PositionRepository {
    @Override
    public List<PositionSnapshot> findOpenPositions(Long memberId) {
        return List.of();
    }

    @Override
    public boolean existsOpenByMemberId(Long memberId) {
        return !findOpenPositions(memberId).isEmpty();
    }

    @Override
    public Optional<PositionSnapshot> findOpenPosition(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return findOpenPositions(memberId).stream()
                .filter(position -> position.symbol().equalsIgnoreCase(symbol))
                .filter(position -> position.positionSide().equalsIgnoreCase(positionSide))
                .filter(position -> position.marginMode().equalsIgnoreCase(marginMode))
                .findFirst();
    }

    @Override
    public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide) {
        return findOpenPositions(memberId).stream()
                .filter(position -> position.symbol().equalsIgnoreCase(symbol))
                .filter(position -> position.positionSide().equalsIgnoreCase(positionSide))
                .findFirst();
    }

    @Override
    public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
        return List.of();
    }

    @Override
    public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
        throw new UnsupportedOperationException("save is not implemented for this test fake");
    }

    @Override
    public PositionMutationResult updateWithVersion(
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

    @Override
    public PositionMutationResult deleteWithVersion(Long memberId, PositionSnapshot expectedPosition) {
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

    @Override
    public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
        throw new UnsupportedOperationException("deleteIfOpen is not implemented for this test fake");
    }

    @Override
    public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
        deleteIfOpen(memberId, symbol, positionSide, marginMode);
    }
}
