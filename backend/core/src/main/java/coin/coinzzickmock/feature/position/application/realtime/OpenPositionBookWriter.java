package coin.coinzzickmock.feature.position.application.realtime;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class OpenPositionBookWriter {
    private final OpenPositionBook openPositionBook;

    public void addAfterCommit(Long memberId, PositionSnapshot position) {
        afterCommit(() -> openPositionBook.add(memberId, position));
    }

    public void replaceAfterCommit(Long memberId, PositionSnapshot position) {
        afterCommit(() -> openPositionBook.replace(memberId, position));
    }

    public void removeAfterCommit(Long memberId, PositionSnapshot position) {
        afterCommit(() -> openPositionBook.remove(memberId, position));
    }

    public void evictSymbolAfterCommit(String symbol) {
        afterCommit(() -> openPositionBook.evictSymbol(symbol));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
