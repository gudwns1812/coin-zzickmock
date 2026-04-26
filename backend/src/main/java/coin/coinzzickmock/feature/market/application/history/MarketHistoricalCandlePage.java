package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import java.util.List;

public record MarketHistoricalCandlePage(List<MarketCandleResult> candles) {
}
