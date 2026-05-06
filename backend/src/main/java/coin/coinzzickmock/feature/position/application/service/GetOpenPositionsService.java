package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.position.application.query.PositionSnapshotResultAssembler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOpenPositionsService {
    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final PositionSnapshotResultAssembler positionSnapshotResultAssembler;

    @Transactional(readOnly = true)
    public List<PositionSnapshotResult> getPositions(Long memberId) {
        List<PositionSnapshot> markedPositions = positionRepository.findOpenPositions(memberId).stream()
                .map(this::markToMarketForRead)
                .toList();
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        return markedPositions.stream()
                .map(snapshot -> positionSnapshotResultAssembler.assemble(
                        memberId,
                        account.walletBalance(),
                        markedPositions,
                        snapshot
                ))
                .toList();
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        return realtimeMarketPriceReader.freshMarkPrice(snapshot.symbol())
                .map(snapshot::markToMarket)
                .orElse(snapshot);
    }
}
