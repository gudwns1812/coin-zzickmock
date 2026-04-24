package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRetryProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryRetryScheduler {
    private final MarketHistoryRetryProcessor marketHistoryRetryProcessor;

    @Scheduled(fixedDelayString = "${coin.market.history-retry-delay-ms:5000}")
    public void retryPendingHistory() {
        marketHistoryRetryProcessor.retryPending();
    }
}
