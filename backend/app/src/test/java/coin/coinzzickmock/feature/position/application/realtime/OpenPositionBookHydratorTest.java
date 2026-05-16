package coin.coinzzickmock.feature.position.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.order.application.implement.OrderMarketTradeMovementWorker;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingLimitOrderBookHydrator;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.testsupport.TestPositionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenPositionBookHydratorTest {
    @Test
    void startHydratesAllOpenCandidatesBeforeMovementWorkers() {
        OpenPositionBook book = new OpenPositionBook();
        OpenPositionBookHydrator hydrator = new OpenPositionBookHydrator(repository(
                candidate(1L, "BTCUSDT"),
                candidate(2L, "ETHUSDT")
        ), book);

        hydrator.start();

        assertThat(hydrator.isRunning()).isTrue();
        assertThat(hydrator.getPhase()).isGreaterThan(OrderPendingLimitOrderBookHydrator.PHASE);
        assertThat(hydrator.getPhase()).isLessThan(OrderMarketTradeMovementWorker.PHASE);
        assertThat(book.candidatesBySymbol("BTCUSDT").values()).extracting(OpenPositionCandidate::memberId)
                .containsExactly(1L);
    }

    @Test
    void rehydrateSymbolEvictsOnlyThatSymbolAndReloadsFreshCandidates() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of(candidate(1L, "BTCUSDT"), candidate(2L, "ETHUSDT")));
        OpenPositionBookHydrator hydrator = new OpenPositionBookHydrator(repository(candidate(3L, "BTCUSDT")), book);

        hydrator.rehydrateSymbol("BTCUSDT");

        assertThat(book.candidatesBySymbol("BTCUSDT").values()).extracting(OpenPositionCandidate::memberId)
                .containsExactly(3L);
        assertThat(book.candidatesBySymbol("ETHUSDT").values()).extracting(OpenPositionCandidate::memberId)
                .containsExactly(2L);
    }

    private TestPositionRepository repository(OpenPositionCandidate... candidates) {
        return new TestPositionRepository() {
            @Override
            public List<OpenPositionCandidate> findAllOpenCandidates() {
                return List.of(candidates);
            }

            @Override
            public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
                return List.of(candidates).stream()
                        .filter(candidate -> candidate.symbol().equalsIgnoreCase(symbol))
                        .toList();
            }
        };
    }

    private OpenPositionCandidate candidate(Long memberId, String symbol) {
        return new OpenPositionCandidate(
                memberId,
                PositionSnapshot.open(symbol, "LONG", "ISOLATED", 10, 1, 100, 100)
        );
    }
}
