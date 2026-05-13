package coin.coinzzickmock.feature.positionpeek.application.service;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekPublicPositionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PublicPositionSnapshotReader {
    private final PositionRepository positionRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;

    @Transactional(readOnly = true)
    public List<PositionPeekPublicPositionResult> read(Long targetMemberId) {
        return positionRepository.findOpenPositions(targetMemberId).stream()
                .map(this::markToMarketForRead)
                .map(snapshot -> new PositionPeekPublicPositionResult(
                        snapshot.symbol(),
                        snapshot.positionSide(),
                        snapshot.leverage(),
                        snapshot.quantity(),
                        Math.abs(snapshot.quantity() * snapshot.markPrice()),
                        snapshot.unrealizedPnl(),
                        snapshot.roi()
                ))
                .toList();
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        return realtimeMarketPriceReader.freshMarkPrice(snapshot.symbol())
                .map(snapshot::markToMarket)
                .orElse(snapshot);
    }
}
