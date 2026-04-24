package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.MarketMinuteClosedEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMinuteBoundaryEventPublisher {
    private static final ZoneOffset HISTORY_ZONE = ZoneOffset.UTC;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(cron = "${coin.market.minute-close-cron:0 * * * * *}")
    public void publishMinuteClosedEvent() {
        publishMinuteClosedEvent(Instant.now());
    }

    void publishMinuteClosedEvent(Instant observedAt) {
        Instant closeTime = ZonedDateTime.ofInstant(observedAt, HISTORY_ZONE)
                .truncatedTo(ChronoUnit.MINUTES)
                .toInstant();
        Instant openTime = closeTime.minus(1, ChronoUnit.MINUTES);
        applicationEventPublisher.publishEvent(new MarketMinuteClosedEvent(openTime, closeTime));
    }
}
