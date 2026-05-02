package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryStartupBackfill;
import coin.coinzzickmock.providers.Providers;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "coin.market.startup-backfill",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MarketHistoryStartupBackfillReadyEventListener {
    private final MarketHistoryStartupBackfill marketHistoryStartupBackfill;
    private final Providers providers;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissingHistoryAfterApplicationReady() {
        marketHistoryStartupBackfill.backfillMissingMinuteHistory(
                Instant.now(),
                providers.connector().marketDataGateway()
        );
    }
}
