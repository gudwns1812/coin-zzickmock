package coin.coinzzickmock.feature.position.application.implement;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenPositionBookHydrator {
    private final PositionRepository positionRepository;
    private final OpenPositionBook openPositionBook;

    public void hydrate() {
        openPositionBook.hydrate(positionRepository.findAllOpenCandidates());
    }

    public void rehydrateSymbol(String symbol) {
        long dirtyGeneration = openPositionBook.evictSymbol(symbol);
        openPositionBook.replaceSymbolIfDirtyGeneration(symbol, positionRepository.findOpenBySymbol(symbol), dirtyGeneration);
    }
}
