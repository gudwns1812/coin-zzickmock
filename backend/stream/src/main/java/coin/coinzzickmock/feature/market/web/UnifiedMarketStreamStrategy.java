package coin.coinzzickmock.feature.market.web;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnifiedMarketStreamStrategy implements MarketSseStreamStrategy {
    private final MarketStreamBroker marketStreamBroker;
    private final MarketOpenPositionSymbolsReader openPositionSymbolsReader;
    private final MarketStreamActorReader marketStreamActorReader;

    @Override
    public MarketSseStreamKind kind() {
        return MarketSseStreamKind.UNIFIED;
    }

    @Override
    public void open(MarketSseStreamRequest request) {
        Long memberId = marketStreamActorReader.currentMemberId().orElse(null);
        Set<String> openSymbols = memberId == null
                ? Set.of()
                : new LinkedHashSet<>(openPositionSymbolsReader.openSymbols(memberId));
        marketStreamBroker.openSession(
                memberId,
                request.clientKey(),
                request.activeSymbol(),
                openSymbols,
                request.candleInterval(),
                request.emitter()
        );
    }
}
