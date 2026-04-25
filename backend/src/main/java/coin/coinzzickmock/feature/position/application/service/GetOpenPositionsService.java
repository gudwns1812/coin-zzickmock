package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOpenPositionsService {
    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<PositionSnapshotResult> getPositions(String memberId) {
        return positionRepository.findOpenPositions(memberId).stream()
                .map(this::toResult)
                .toList();
    }

    private PositionSnapshotResult toResult(PositionSnapshot snapshot) {
        return new PositionSnapshotResult(
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode(),
                snapshot.leverage(),
                snapshot.quantity(),
                snapshot.entryPrice(),
                snapshot.markPrice(),
                snapshot.liquidationPrice(),
                snapshot.unrealizedPnl(),
                snapshot.initialMargin(),
                snapshot.roi()
        );
    }
}
