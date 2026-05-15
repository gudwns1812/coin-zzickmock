package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTickerEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTradeEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ProviderMarketRealtimeEventBridgeTest {
    @Test
    void routesParsedTradeTickerAndCandleEventsIntoRealtimeState() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        List<Object> publishedEvents = new ArrayList<>();
        List<MarketTradePriceMovedEvent> movements = new ArrayList<>();
        ApplicationEventPublisher publisher = publishedEvents::add;
        ProviderMarketRealtimeEventBridge bridge = new ProviderMarketRealtimeEventBridge(
                store,
                new RealtimeMarketCandleUpdateService(store, publisher),
                event -> {
                    movements.add(event);
                    return true;
                }
        );
        Instant sourceEventTime = Instant.parse("2026-04-30T05:30:00Z");
        Instant receivedAt = Instant.parse("2026-04-30T05:30:01Z");

        bridge.accept(new ProviderMarketTradeEvent(
                "BTCUSDT",
                "trade-1",
                new BigDecimal("75300.1"),
                new BigDecimal("0.01"),
                "buy",
                sourceEventTime,
                receivedAt
        ));
        bridge.accept(new ProviderMarketTradeEvent(
                "BTCUSDT",
                "trade-2",
                new BigDecimal("75299.1"),
                new BigDecimal("0.01"),
                "sell",
                sourceEventTime.plusMillis(1),
                receivedAt
        ));
        bridge.accept(new ProviderMarketTickerEvent(
                "BTCUSDT",
                new BigDecimal("75301.2"),
                new BigDecimal("75302.3"),
                new BigDecimal("75303.4"),
                new BigDecimal("0.00001"),
                Instant.parse("2026-04-30T08:00:00Z"),
                sourceEventTime,
                receivedAt
        ));
        bridge.accept(new ProviderMarketCandleEvent(
                "BTCUSDT",
                ProviderMarketCandleInterval.ONE_MINUTE,
                Instant.parse("2026-04-30T05:30:00Z"),
                new BigDecimal("75300"),
                new BigDecimal("75310"),
                new BigDecimal("75290"),
                new BigDecimal("75305"),
                new BigDecimal("3.5"),
                new BigDecimal("263500"),
                new BigDecimal("263500"),
                sourceEventTime,
                receivedAt
        ));

        assertThat(store.latestTrade("BTCUSDT")).hasValueSatisfying(trade ->
                assertThat(trade.price()).isEqualByComparingTo("75299.1")
        );
        assertThat(store.latestTicker("BTCUSDT")).hasValueSatisfying(ticker ->
                assertThat(ticker.markPrice()).isEqualByComparingTo("75302.3")
        );
        assertThat(store.latestCandle("BTCUSDT", MarketCandleInterval.ONE_MINUTE)).hasValueSatisfying(candle ->
                assertThat(candle.closePrice()).isEqualByComparingTo("75305")
        );
        assertThat(publishedEvents).containsExactly(new MarketCandleUpdatedEvent("BTCUSDT"));
        assertThat(movements).containsExactly(new MarketTradePriceMovedEvent(
                "BTCUSDT",
                75300.1,
                75299.1,
                MarketPriceMovementDirection.DOWN,
                sourceEventTime.plusMillis(1),
                receivedAt
        ));
    }
}
