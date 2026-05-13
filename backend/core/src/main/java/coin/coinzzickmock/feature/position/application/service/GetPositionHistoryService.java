package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.result.PositionHistoryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPositionHistoryService {
    private final PositionHistoryRepository positionHistoryRepository;

    @Transactional(readOnly = true)
    public List<PositionHistoryResult> getHistory(Long memberId, String symbol) {
        return positionHistoryRepository.findByMemberId(memberId, symbol).stream()
                .map(PositionHistoryResult::from)
                .toList();
    }
}
