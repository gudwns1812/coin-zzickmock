package coin.coinzzickmock.feature.position.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenPositionBookTest {
    @Test
    void hydrateIndexesAllCandidatesAndFiltersBySymbol() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of(
                candidate(1L, "BTCUSDT", "LONG", "ISOLATED"),
                candidate(2L, "ETHUSDT", "SHORT", "CROSS")
        ));

        OpenPositionBook.Candidates btc = book.candidatesBySymbol("btcusdt");

        assertThat(btc.state()).isEqualTo(OpenPositionBook.State.CLEAN);
        assertThat(btc.values()).extracting(OpenPositionCandidate::memberId).containsExactly(1L);
        assertThat(book.size()).isEqualTo(2);
    }

    @Test
    void addReplaceRemoveAndEvictSymbolKeepIndexCurrent() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of());
        PositionSnapshot initial = position("BTCUSDT", "LONG", "ISOLATED", 1);
        PositionSnapshot replacement = position("BTCUSDT", "LONG", "ISOLATED", 2);

        book.add(1L, initial);
        book.replace(1L, replacement);

        assertThat(book.candidatesBySymbol("BTCUSDT").values())
                .extracting(candidate -> candidate.position().quantity())
                .containsExactly(2d);

        book.remove(1L, replacement);
        assertThat(book.candidatesBySymbol("BTCUSDT").values()).isEmpty();

        book.add(1L, replacement);
        book.evictSymbol("BTCUSDT");
        assertThat(book.candidatesBySymbol("BTCUSDT").state()).isEqualTo(OpenPositionBook.State.DIRTY);
        assertThat(book.candidatesBySymbol("BTCUSDT").values()).isEmpty();
    }

    @Test
    void unhydratedBookReportsUnhydratedSoCallerCanRehydrateBeforeUsingCandidates() {
        OpenPositionBook book = new OpenPositionBook();

        OpenPositionBook.Candidates candidates = book.candidatesBySymbol("BTCUSDT");

        assertThat(candidates.state()).isEqualTo(OpenPositionBook.State.UNHYDRATED);
        assertThat(candidates.requiresRehydrate()).isTrue();
    }


    @Test
    void staleRehydrateDoesNotCleanNewerDirtyGeneration() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of(candidate(1L, "BTCUSDT", "LONG", "ISOLATED")));

        long staleGeneration = book.evictSymbol("BTCUSDT");
        book.add(candidate(2L, "BTCUSDT", "LONG", "ISOLATED"));

        boolean applied = book.replaceSymbolIfDirtyGeneration(
                "BTCUSDT",
                List.of(candidate(3L, "BTCUSDT", "LONG", "ISOLATED")),
                staleGeneration
        );

        OpenPositionBook.Candidates candidates = book.candidatesBySymbol("BTCUSDT");
        assertThat(applied).isFalse();
        assertThat(candidates.state()).isEqualTo(OpenPositionBook.State.DIRTY);
        assertThat(candidates.values()).extracting(OpenPositionCandidate::memberId).containsExactly(2L);
    }

    @Test
    void markCleanOnlyClearsMatchingDirtyGeneration() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of(candidate(1L, "BTCUSDT", "LONG", "ISOLATED")));

        long generation = book.evictSymbol("BTCUSDT");
        boolean applied = book.replaceSymbolIfDirtyGeneration(
                "BTCUSDT",
                List.of(candidate(2L, "BTCUSDT", "LONG", "ISOLATED")),
                generation
        );

        OpenPositionBook.Candidates candidates = book.candidatesBySymbol("BTCUSDT");
        assertThat(applied).isTrue();
        assertThat(candidates.state()).isEqualTo(OpenPositionBook.State.CLEAN);
        assertThat(candidates.values()).extracting(OpenPositionCandidate::memberId).containsExactly(2L);
    }


    @Test
    void writerUpdateDuringRehydrateKeepsSymbolDirtyAndDoesNotOverwriteNewerCandidate() {
        OpenPositionBook book = new OpenPositionBook();
        book.hydrate(List.of(candidate(1L, "BTCUSDT", "LONG", "ISOLATED")));

        long rehydrateGeneration = book.evictSymbol("BTCUSDT");
        book.replace(1L, position("BTCUSDT", "LONG", "ISOLATED", 7));

        boolean applied = book.replaceSymbolIfDirtyGeneration(
                "BTCUSDT",
                List.of(candidate(1L, "BTCUSDT", "LONG", "ISOLATED")),
                rehydrateGeneration
        );

        OpenPositionBook.Candidates candidates = book.candidatesBySymbol("BTCUSDT");
        assertThat(applied).isFalse();
        assertThat(candidates.state()).isEqualTo(OpenPositionBook.State.DIRTY);
        assertThat(candidates.values()).extracting(candidate -> candidate.position().quantity()).containsExactly(7d);
    }

    private OpenPositionCandidate candidate(Long memberId, String symbol, String positionSide, String marginMode) {
        return new OpenPositionCandidate(memberId, position(symbol, positionSide, marginMode, 1));
    }

    private PositionSnapshot position(String symbol, String positionSide, String marginMode, double quantity) {
        return PositionSnapshot.open(symbol, positionSide, marginMode, 10, quantity, 100, 100);
    }
}
