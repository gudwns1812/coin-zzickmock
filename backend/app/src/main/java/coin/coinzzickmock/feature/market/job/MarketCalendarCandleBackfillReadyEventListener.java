package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.application.history.MarketCalendarCandleBackfill;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "coin.market.calendar-candle-backfill",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MarketCalendarCandleBackfillReadyEventListener {
    private final MarketCalendarCandleBackfill marketCalendarCandleBackfill;

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpPersistedCalendarCandlesAfterApplicationReady() {
        marketCalendarCandleBackfill.catchUpPersistedCalendarCandles();
    }
}
