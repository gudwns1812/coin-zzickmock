package coin.coinzzickmock.feature.position.application.realtime;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenPositionBookHydrator implements SmartLifecycle {
    public static final int PHASE = Integer.MIN_VALUE + 110;

    private final PositionRepository positionRepository;
    private final OpenPositionBook openPositionBook;
    private volatile boolean running;

    @Override
    public void start() {
        hydrate();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    public void hydrate() {
        openPositionBook.hydrate(positionRepository.findAllOpenCandidates());
    }

    public void rehydrateSymbol(String symbol) {
        long dirtyGeneration = openPositionBook.evictSymbol(symbol);
        openPositionBook.replaceSymbolIfDirtyGeneration(symbol, positionRepository.findOpenBySymbol(symbol), dirtyGeneration);
    }
}
