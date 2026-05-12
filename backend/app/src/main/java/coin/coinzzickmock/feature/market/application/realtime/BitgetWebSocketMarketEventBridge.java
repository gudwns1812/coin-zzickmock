package coin.coinzzickmock.feature.market.application.realtime;

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
