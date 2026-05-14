package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketRealtimeEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTickerEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTradeEvent;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderMarketRealtimeEventBridge implements Consumer<ProviderMarketRealtimeEvent> {
    private static final Logger log = LoggerFactory.getLogger(ProviderMarketRealtimeEventBridge.class);
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService;
    private final MarketTradePriceMovementPublisher marketTradePriceMovementPublisher;

    public ProviderMarketRealtimeEventBridge(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService,
            MarketTradePriceMovementPublisher marketTradePriceMovementPublisher
    ) {
        this.realtimeMarketDataStore = realtimeMarketDataStore;
        this.realtimeMarketCandleUpdateService = realtimeMarketCandleUpdateService;
        this.marketTradePriceMovementPublisher = marketTradePriceMovementPublisher;
    }

    @Override
    public void accept(ProviderMarketRealtimeEvent event) {
        if (event instanceof ProviderMarketTradeEvent trade) {
            realtimeMarketDataStore.acceptTradeUpdate(new RealtimeMarketTradeTick(
                    trade.symbol(),
                    trade.tradeId(),
                    trade.price(),
                    trade.size(),
                    trade.side(),
                    trade.sourceEventTime(),
                    trade.receivedAt()
            )).movement().ifPresent(movement -> {
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
            realtimeMarketDataStore.acceptTicker(new RealtimeMarketTickerUpdate(
                    ticker.symbol(),
                    ticker.lastPrice(),
                    ticker.markPrice(),
                    ticker.indexPrice(),
                    ticker.fundingRate(),
                    ticker.nextFundingTime(),
                    ticker.sourceEventTime(),
                    ticker.receivedAt()
            ));
            return;
        }

        if (event instanceof ProviderMarketCandleEvent candle) {
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
        }
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
