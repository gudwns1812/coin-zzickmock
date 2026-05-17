package coin.coinzzickmock.feature.position.job;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.order.application.implement.OrderMarketTradeMovementWorker;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingLimitOrderBookHydrator;
import coin.coinzzickmock.feature.position.application.dto.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.implement.OpenPositionBook;
import coin.coinzzickmock.feature.position.application.implement.OpenPositionBookHydrator;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.testsupport.TestPositionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenPositionBookHydrationLifecycleTest {
    @Test
    void startHydratesOpenPositionBookBeforeMovementWorkers() {
        OpenPositionBook book = new OpenPositionBook();
        OpenPositionBookHydrator hydrator = new OpenPositionBookHydrator(repository(), book);
        OpenPositionBookHydrationLifecycle lifecycle = new OpenPositionBookHydrationLifecycle(hydrator);

        lifecycle.start();

        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(lifecycle.getPhase()).isGreaterThan(OrderPendingLimitOrderBookHydrator.PHASE);
        assertThat(lifecycle.getPhase()).isLessThan(OrderMarketTradeMovementWorker.PHASE);
        assertThat(book.candidatesBySymbol("BTCUSDT").values()).extracting(OpenPositionCandidate::memberId)
                .containsExactly(1L);
    }

    private TestPositionRepository repository() {
        return new TestPositionRepository() {
            @Override
            public List<OpenPositionCandidate> findAllOpenCandidates() {
                return List.of(new OpenPositionCandidate(
                        1L,
                        PositionSnapshot.open("BTCUSDT", "LONG", "ISOLATED", 10, 1, 100, 100)
                ));
            }
        };
    }
}
