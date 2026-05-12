package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.position.application.query.OpenPositionSymbolsReader;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnifiedMarketStreamStrategy implements MarketSseStreamStrategy {
    private final MarketStreamBroker marketStreamBroker;
    private final OpenPositionSymbolsReader openPositionSymbolsReader;
    private final Providers providers;

    @Override
    public MarketSseStreamKind kind() {
        return MarketSseStreamKind.UNIFIED;
    }

    @Override
    public void open(MarketSseStreamRequest request) {
        Actor actor = providers.auth().currentActorOptional().orElse(null);
        Long memberId = actor == null ? null : actor.memberId();
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
