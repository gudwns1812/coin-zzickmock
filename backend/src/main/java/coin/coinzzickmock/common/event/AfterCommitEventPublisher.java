package coin.coinzzickmock.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AfterCommitEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applicationEventPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationEventPublisher.publishEvent(event);
            }
        });
    }
}
