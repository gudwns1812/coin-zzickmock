package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultConnectorProvider implements ConnectorProvider {
    private final MarketDataGateway marketDataGateway;

    @Override
    public MarketDataGateway marketDataGateway() {
        return marketDataGateway;
    }
}
