package coin.coinzzickmock.feature.market.web;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketSseStreamRouter {
    private final Map<MarketSseStreamKind, MarketSseStreamStrategy> strategies;

    public MarketSseStreamRouter(List<MarketSseStreamStrategy> strategies) {
        EnumMap<MarketSseStreamKind, MarketSseStreamStrategy> resolvedStrategies =
                new EnumMap<>(MarketSseStreamKind.class);
        for (MarketSseStreamStrategy strategy : strategies) {
            MarketSseStreamStrategy previous = resolvedStrategies.put(strategy.kind(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate market SSE stream strategy: " + strategy.kind());
            }
        }
        EnumSet<MarketSseStreamKind> missingKinds = EnumSet.allOf(MarketSseStreamKind.class);
        missingKinds.removeAll(resolvedStrategies.keySet());
        if (!missingKinds.isEmpty()) {
            throw new IllegalStateException("Missing market SSE stream strategies: " + missingKinds);
        }
        this.strategies = Map.copyOf(resolvedStrategies);
    }

    public void open(MarketSseStreamRequest request) {
        MarketSseStreamStrategy strategy = strategies.get(request.kind());
        if (strategy == null) {
            throw new IllegalStateException("No market SSE stream strategy for kind: " + request.kind());
        }
        strategy.open(request);
    }
}
