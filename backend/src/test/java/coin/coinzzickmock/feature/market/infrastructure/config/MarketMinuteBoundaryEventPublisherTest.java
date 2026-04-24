package coin.coinzzickmock.feature.market.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import coin.coinzzickmock.feature.market.application.realtime.MarketMinuteClosedEvent;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class MarketMinuteBoundaryEventPublisherTest {
    @Test
    void publishesJustClosedMinuteAtObservedMinuteBoundary() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketMinuteBoundaryEventPublisher publisher = new MarketMinuteBoundaryEventPublisher(applicationEventPublisher);

        publisher.publishMinuteClosedEvent(Instant.parse("2026-04-17T06:01:42Z"));

        MarketMinuteClosedEvent event = assertInstanceOf(
                MarketMinuteClosedEvent.class,
                applicationEventPublisher.events().get(0)
        );
        assertEquals(Instant.parse("2026-04-17T06:00:00Z"), event.openTime());
        assertEquals(Instant.parse("2026-04-17T06:01:00Z"), event.closeTime());
    }

    private static class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> events = new CopyOnWriteArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }

        private List<Object> events() {
            return List.copyOf(events);
        }
    }
}
