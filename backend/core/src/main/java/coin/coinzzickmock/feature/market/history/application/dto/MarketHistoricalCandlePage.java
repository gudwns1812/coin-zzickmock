package coin.coinzzickmock.feature.market.history.application.dto;

import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleResult;
import java.util.List;

public record MarketHistoricalCandlePage(List<MarketCandleResult> candles) {
}
