package coin.coinzzickmock.feature.push.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.candle.application.dto.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.candle.application.implement.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.quote.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import coin.coinzzickmock.feature.push.application.dto.PushEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketPushEventPublicationBridgeTest {
    private final PushEventEnvelopeFactory envelopeFactory = new PushEventEnvelopeFactory(
            new ObjectMapper().findAndRegisterModules(),
            new PushPublicationProperties(
                    "coin:push:market:v1",
                    "coin:push:trading:v1",
                    10_000,
                    Duration.ofSeconds(15),
                    Duration.ofSeconds(5)
            )
    );

    @Test
    void publishes_projected_unified_candles_for_minute_derived_intervals() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        List<PushEventEnvelope> published = new ArrayList<>();
        MarketPushEventPublicationBridge bridge = new MarketPushEventPublicationBridge(
                (symbol, openTime, closeTime) -> List.of(),
                projector,
                published::add,
                envelopeFactory
        );
        Instant openTime = Instant.parse("2026-05-29T00:01:00Z");
        RealtimeMarketCandleUpdate update = candle(
                MarketCandleInterval.ONE_MINUTE,
                openTime,
                100,
                102,
                99,
                101,
                1.5
        );
        store.acceptCandle(update);

        bridge.onCandleUpdated(MarketCandleUpdatedEvent.from(update));

        assertThat(published)
                .filteredOn(envelope -> envelope.eventType() == PushEventType.MARKET_UNIFIED_CANDLE)
                .extracting(PushEventEnvelope::interval)
                .containsExactly("1m", "3m", "5m", "15m");
        assertThat(published)
                .filteredOn(envelope -> envelope.eventType() == PushEventType.MARKET_CANDLE)
                .extracting(PushEventEnvelope::interval)
                .containsExactly("1m", "3m", "5m", "15m");
    }

    private static RealtimeMarketCandleUpdate candle(
            MarketCandleInterval interval,
            Instant openTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        return new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                interval,
                openTime,
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(volume),
                BigDecimal.valueOf(volume * close),
                BigDecimal.valueOf(volume * close),
                openTime.plusSeconds(1),
                openTime.plusSeconds(1)
        );
    }
}
