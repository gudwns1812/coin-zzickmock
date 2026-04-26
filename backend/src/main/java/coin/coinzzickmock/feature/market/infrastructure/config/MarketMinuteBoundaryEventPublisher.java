package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.MarketMinuteClosedEvent;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMinuteBoundaryEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(cron = "${coin.market.minute-close-cron:0 * * * * *}")
    public void publishMinuteClosedEvent() {
        publishMinuteClosedEvent(Instant.now());
    }

    void publishMinuteClosedEvent(Instant observedAt) {
        Instant closeTime = MarketTime.truncate(observedAt, ChronoUnit.MINUTES);
        Instant openTime = closeTime.minus(1, ChronoUnit.MINUTES);
        applicationEventPublisher.publishEvent(new MarketMinuteClosedEvent(openTime, closeTime));
    }
}
