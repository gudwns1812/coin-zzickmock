package coin.coinzzickmock.feature.position.application.realtime;

import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class OpenPositionBook {
    private final Map<PositionKey, OpenPositionCandidate> candidates = new HashMap<>();
    private final Map<String, Map<PositionKey, OpenPositionCandidate>> candidatesBySymbol = new HashMap<>();
    private final Map<String, Long> dirtySymbolGenerations = new HashMap<>();
    private final AtomicLong dirtyGenerationSequence = new AtomicLong();
    private volatile boolean hydrated;

    public synchronized void hydrate(Collection<OpenPositionCandidate> openPositions) {
        candidates.clear();
        candidatesBySymbol.clear();
        openPositions.forEach(this::putCandidate);
        dirtySymbolGenerations.clear();
        hydrated = true;
    }

    public synchronized Candidates candidatesBySymbol(String symbol) {
        String normalizedSymbol = normalize(symbol);
        State state = stateOf(normalizedSymbol);
        Map<PositionKey, OpenPositionCandidate> symbolCandidates = candidatesBySymbol.get(normalizedSymbol);
        List<OpenPositionCandidate> values = symbolCandidates == null
                ? List.of()
                : List.copyOf(symbolCandidates.values());
        return new Candidates(state, values);
    }

    public synchronized void add(Long memberId, PositionSnapshot position) {
        add(new OpenPositionCandidate(memberId, position));
    }

    public synchronized void add(OpenPositionCandidate candidate) {
        putCandidate(candidate);
        bumpDirtyGenerationIfPresent(candidate.symbol());
    }

    public synchronized void replace(Long memberId, PositionSnapshot position) {
        String normalizedSymbol = normalize(position.symbol());
        removeCandidate(new PositionKey(memberId, normalizedSymbol, normalize(position.positionSide()), normalize(position.marginMode())));
        putCandidate(new OpenPositionCandidate(memberId, position));
        bumpDirtyGenerationIfPresent(normalizedSymbol);
    }

    public synchronized void remove(Long memberId, PositionSnapshot position) {
        remove(memberId, position.symbol(), position.positionSide(), position.marginMode());
    }

    public synchronized void remove(Long memberId, String symbol, String positionSide, String marginMode) {
        String normalizedSymbol = normalize(symbol);
        removeCandidate(new PositionKey(memberId, normalizedSymbol, normalize(positionSide), normalize(marginMode)));
        bumpDirtyGenerationIfPresent(normalizedSymbol);
    }

    public synchronized long evictSymbol(String symbol) {
        String normalizedSymbol = normalize(symbol);
        removeSymbolCandidates(normalizedSymbol);
        if (!hydrated) {
            return 0;
        }
        long generation = dirtyGenerationSequence.incrementAndGet();
        dirtySymbolGenerations.put(normalizedSymbol, generation);
        return generation;
    }

    boolean replaceSymbolIfDirtyGeneration(String symbol, Collection<OpenPositionCandidate> openPositions, long observedDirtyGeneration) {
        String normalizedSymbol = normalize(symbol);
        synchronized (this) {
            Long currentDirtyGeneration = dirtySymbolGenerations.get(normalizedSymbol);
            if (!hydrated || currentDirtyGeneration == null || currentDirtyGeneration != observedDirtyGeneration) {
                return false;
            }
            removeSymbolCandidates(normalizedSymbol);
            openPositions.forEach(this::putCandidate);
            dirtySymbolGenerations.remove(normalizedSymbol, observedDirtyGeneration);
            return true;
        }
    }

    public synchronized int size() {
        return candidates.size();
    }

    private void putCandidate(OpenPositionCandidate candidate) {
        PositionKey key = PositionKey.from(candidate);
        candidates.put(key, candidate);
        candidatesBySymbol.computeIfAbsent(key.symbol(), ignored -> new HashMap<>())
                .put(key, candidate);
    }

    private void bumpDirtyGenerationIfPresent(String symbol) {
        String normalizedSymbol = normalize(symbol);
        if (hydrated && dirtySymbolGenerations.containsKey(normalizedSymbol)) {
            dirtySymbolGenerations.put(normalizedSymbol, dirtyGenerationSequence.incrementAndGet());
        }
    }

    private void removeSymbolCandidates(String normalizedSymbol) {
        Map<PositionKey, OpenPositionCandidate> removedCandidates = candidatesBySymbol.remove(normalizedSymbol);
        if (removedCandidates == null) {
            return;
        }
        removedCandidates.keySet().forEach(candidates::remove);
    }

    private void removeCandidate(PositionKey key) {
        candidates.remove(key);
        Map<PositionKey, OpenPositionCandidate> symbolCandidates = candidatesBySymbol.get(key.symbol());
        if (symbolCandidates == null) {
            return;
        }
        symbolCandidates.remove(key);
        if (symbolCandidates.isEmpty()) {
            candidatesBySymbol.remove(key.symbol());
        }
    }

    private State stateOf(String normalizedSymbol) {
        if (!hydrated) {
            return State.UNHYDRATED;
        }
        if (dirtySymbolGenerations.containsKey(normalizedSymbol)) {
            return State.DIRTY;
        }
        return State.CLEAN;
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    public enum State {
        CLEAN,
        DIRTY,
        UNHYDRATED
    }

    public record Candidates(State state, List<OpenPositionCandidate> values) {
        public boolean requiresRehydrate() {
            return state != State.CLEAN;
        }
    }

    private record PositionKey(Long memberId, String symbol, String positionSide, String marginMode) {
        private static PositionKey from(OpenPositionCandidate candidate) {
            return new PositionKey(
                    candidate.memberId(),
                    normalize(candidate.symbol()),
                    normalize(candidate.positionSide()),
                    normalize(candidate.marginMode())
            );
        }
    }
}
