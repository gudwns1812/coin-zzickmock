package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketCandleEvent;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEvent;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketTickerEvent;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketTradeEvent;
import java.util.function.Consumer;
public class BitgetWebSocketMarketEventBridge implements Consumer<BitgetWebSocketMarketEvent> {
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService;

    public BitgetWebSocketMarketEventBridge(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService
    ) {
        this.realtimeMarketDataStore = realtimeMarketDataStore;
        this.realtimeMarketCandleUpdateService = realtimeMarketCandleUpdateService;
    }

    @Override
    public void accept(BitgetWebSocketMarketEvent event) {
        if (event instanceof BitgetWebSocketTradeEvent trade) {
            realtimeMarketDataStore.acceptTrade(new RealtimeMarketTradeTick(
                    trade.symbol(),
                    trade.tradeId(),
                    trade.price(),
                    trade.size(),
                    trade.side(),
                    trade.sourceEventTime(),
                    trade.receivedAt()
            ));
            return;
        }

        if (event instanceof BitgetWebSocketTickerEvent ticker) {
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

        if (event instanceof BitgetWebSocketCandleEvent candle) {
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
