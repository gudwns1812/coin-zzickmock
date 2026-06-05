package coin.coinzzickmock.feature.market.quote.application.service;

import coin.coinzzickmock.feature.market.candle.application.dto.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.candle.application.service.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.quote.application.implement.MarketTradePriceMovementPublisher;
import coin.coinzzickmock.feature.market.quote.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.quote.application.dto.RealtimeMarketTradeAcceptance;
import coin.coinzzickmock.feature.market.quote.application.dto.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.quote.application.dto.RealtimeMarketTradeTick;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketRealtimeEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTickerEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTradeEvent;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ProviderMarketRealtimeEventBridge implements Consumer<ProviderMarketRealtimeEvent> {
    private static final String PROVIDER_EVENT_TOTAL = "market.realtime.provider.event.total";
    private static final String STORE_UPDATE_TOTAL = "market.realtime.store.update.total";

    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService;
    private final MarketTradePriceMovementPublisher marketTradePriceMovementPublisher;
    private final TelemetryProvider telemetryProvider;

    @Override
    public void accept(ProviderMarketRealtimeEvent event) {
        if (event instanceof ProviderMarketTradeEvent trade) {
            recordProviderEvent("trade");
            RealtimeMarketTradeAcceptance update = realtimeMarketDataStore.acceptTradeUpdate(
                    new RealtimeMarketTradeTick(
                            trade.symbol(),
                            trade.tradeId(),
                            trade.price(),
                            trade.size(),
                            trade.side(),
                            trade.sourceEventTime(),
                            trade.receivedAt()
                    ));
            recordStoreUpdate("trade", update.accepted() ? "accepted" : "rejected");
            update.movement().ifPresent(movement -> {
                boolean published = marketTradePriceMovementPublisher.publish(movement);
                if (!published) {
                    log.warn("Market trade movement queue rejected event. symbol={} sourceEventTime={}",
                            movement.symbol(),
                            movement.sourceEventTime());
                }
            });
            return;
        }

        if (event instanceof ProviderMarketTickerEvent ticker) {
            recordProviderEvent("ticker");
            boolean accepted = realtimeMarketDataStore.acceptTicker(new RealtimeMarketTickerUpdate(
                    ticker.symbol(),
                    ticker.lastPrice(),
                    ticker.markPrice(),
                    ticker.indexPrice(),
                    ticker.fundingRate(),
                    ticker.nextFundingTime(),
                    ticker.sourceEventTime(),
                    ticker.receivedAt()
            ));
            recordStoreUpdate("ticker", accepted ? "accepted" : "rejected");
            return;
        }

        if (event instanceof ProviderMarketCandleEvent candle) {
            recordProviderEvent("candle");
            realtimeMarketCandleUpdateService.accept(new RealtimeMarketCandleUpdate(
                    candle.symbol(),
                    toDomain(candle.interval()),
                    candle.openTime(),
                    candle.openPrice(),
                    candle.highPrice(),
                    candle.lowPrice(),
                    candle.closePrice(),
                    candle.baseVolume(),
                    candle.quoteVolume(),
                    candle.usdtVolume(),
                    candle.sourceEventTime(),
                    candle.receivedAt()
            ));
            recordStoreUpdate("candle", "accepted");
        }
    }

    private void recordProviderEvent(String channel) {
        telemetryProvider.recordEvent(PROVIDER_EVENT_TOTAL, Map.of("channel", channel));
    }

    private void recordStoreUpdate(String channel, String result) {
        telemetryProvider.recordEvent(STORE_UPDATE_TOTAL, Map.of(
                "channel", channel,
                "result", result
        ));
    }

    private MarketCandleInterval toDomain(ProviderMarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> MarketCandleInterval.ONE_MINUTE;
            case THREE_MINUTES -> MarketCandleInterval.THREE_MINUTES;
            case FIVE_MINUTES -> MarketCandleInterval.FIVE_MINUTES;
            case FIFTEEN_MINUTES -> MarketCandleInterval.FIFTEEN_MINUTES;
            case ONE_HOUR -> MarketCandleInterval.ONE_HOUR;
            case FOUR_HOURS -> MarketCandleInterval.FOUR_HOURS;
            case TWELVE_HOURS -> MarketCandleInterval.TWELVE_HOURS;
            case ONE_DAY -> MarketCandleInterval.ONE_DAY;
            case ONE_WEEK -> MarketCandleInterval.ONE_WEEK;
            case ONE_MONTH -> MarketCandleInterval.ONE_MONTH;
        };
    }
}
