package coin.coinzzickmock.feature.position.application.query;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PositionOpenPositionSymbolsReader implements OpenPositionSymbolsReader {
    private final PositionRepository positionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> openSymbols(Long memberId) {
        return positionRepository.findOpenPositions(memberId).stream()
                .map(position -> position.symbol())
                .distinct()
                .sorted()
                .toList();
    }
}
