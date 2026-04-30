package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import java.util.function.Consumer;

public class BitgetWebSocketMarketEventConsumer implements Consumer<BitgetWebSocketMarketEvent> {
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService;

    public BitgetWebSocketMarketEventConsumer(
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
                    candle.interval(),
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
}
