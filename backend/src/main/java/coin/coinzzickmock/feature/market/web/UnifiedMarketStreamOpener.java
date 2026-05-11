package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.position.application.query.OpenPositionSymbolsReader;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class UnifiedMarketStreamOpener {
    private final MarketStreamBroker marketStreamBroker;
    private final OpenPositionSymbolsReader openPositionSymbolsReader;
    private final Providers providers;

    public void open(
            String activeSymbol,
            String interval,
            String clientKey,
            SseEmitter emitter
    ) {
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        Actor actor = providers.auth().currentActorOptional().orElse(null);
        Long memberId = actor == null ? null : actor.memberId();
        MarketCandleInterval candleInterval = MarketCandleInterval.from(interval);
        Set<String> openSymbols = memberId == null
                ? Set.of()
                : new LinkedHashSet<>(openPositionSymbolsReader.openSymbols(memberId));
        marketStreamBroker.openSession(memberId, resolvedClientKey, activeSymbol, openSymbols, candleInterval, emitter);
    }
}
