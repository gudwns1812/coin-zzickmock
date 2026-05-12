package coin.coinzzickmock.feature.market.web;

import java.time.Instant;
import java.util.List;

public interface MarketFinalizedCandleIntervalsReader {
    List<String> readAffectedIntervals(String symbol, Instant openTime, Instant closeTime);
}
