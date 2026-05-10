package coin.coinzzickmock.feature.position.application.query;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenPositionSymbolsReader {
    private final PositionRepository positionRepository;

    public List<String> readOpenSymbols(Long memberId) {
        return positionRepository.findOpenPositions(memberId)
                .stream()
                .map(position -> position.symbol().toUpperCase())
                .distinct()
                .toList();
    }
}
