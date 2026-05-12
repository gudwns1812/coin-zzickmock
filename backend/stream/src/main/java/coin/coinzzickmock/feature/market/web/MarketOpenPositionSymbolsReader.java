package coin.coinzzickmock.feature.market.web;

import java.util.Set;

public interface MarketOpenPositionSymbolsReader {
    Set<String> openSymbols(Long memberId);
}
