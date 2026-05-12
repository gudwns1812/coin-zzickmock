package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairProcessor;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "coin.market.history-repair.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarketHistoryRepairWorker {
    private final MarketHistoryRepairProcessor marketHistoryRepairProcessor;

    @Value("${coin.market.history-repair.worker.block-timeout-ms:2000}")
    private long blockTimeoutMillis;

    @Scheduled(fixedDelayString = "${coin.market.history-repair.worker.fixed-delay-ms:1000}")
    public void processQueuedRepair() {
        marketHistoryRepairProcessor.processNext(Duration.ofMillis(blockTimeoutMillis));
    }
}
