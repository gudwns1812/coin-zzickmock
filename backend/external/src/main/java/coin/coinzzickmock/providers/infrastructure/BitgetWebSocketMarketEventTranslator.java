package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketRealtimeEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTickerEvent;
import coin.coinzzickmock.providers.connector.ProviderMarketTradeEvent;

public final class BitgetWebSocketMarketEventTranslator {
    private BitgetWebSocketMarketEventTranslator() {
    }

    public static ProviderMarketRealtimeEvent toProviderEvent(BitgetWebSocketMarketEvent event) {
        if (event instanceof BitgetWebSocketTradeEvent trade) {
            return new ProviderMarketTradeEvent(
                    trade.symbol(),
                    trade.tradeId(),
                    trade.price(),
                    trade.size(),
                    trade.side(),
                    trade.sourceEventTime(),
                    trade.receivedAt()
            );
        }
        if (event instanceof BitgetWebSocketTickerEvent ticker) {
            return new ProviderMarketTickerEvent(
                    ticker.symbol(),
                    ticker.lastPrice(),
                    ticker.markPrice(),
                    ticker.indexPrice(),
                    ticker.fundingRate(),
                    ticker.nextFundingTime(),
                    ticker.sourceEventTime(),
                    ticker.receivedAt()
            );
        }
        if (event instanceof BitgetWebSocketCandleEvent candle) {
            return new ProviderMarketCandleEvent(
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
            );
        }
        throw new IllegalArgumentException("Unsupported Bitget websocket market event: " + event.getClass().getName());
    }
}
