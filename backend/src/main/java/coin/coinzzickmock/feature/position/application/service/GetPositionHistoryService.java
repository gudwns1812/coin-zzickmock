package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.result.PositionHistoryResult;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPositionHistoryService {
    private final PositionHistoryRepository positionHistoryRepository;

    @Transactional(readOnly = true)
    public List<PositionHistoryResult> getHistory(String memberId, String symbol) {
        return positionHistoryRepository.findByMemberId(memberId, symbol).stream()
                .map(this::toResult)
                .toList();
    }

    private PositionHistoryResult toResult(PositionHistory history) {
        return new PositionHistoryResult(
                history.symbol(),
                history.positionSide(),
                history.marginMode(),
                history.leverage(),
                history.openedAt(),
                history.averageEntryPrice(),
                history.averageExitPrice(),
                history.positionSize(),
                history.realizedPnl(),
                history.grossRealizedPnl(),
                history.openFee(),
                history.closeFee(),
                history.totalFee(),
                history.fundingCost(),
                history.netRealizedPnl(),
                history.roi(),
                history.closedAt(),
                history.closeReason()
        );
    }
}
