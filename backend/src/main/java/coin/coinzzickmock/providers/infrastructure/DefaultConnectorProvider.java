package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import org.springframework.stereotype.Component;

@Component
public class DefaultConnectorProvider implements ConnectorProvider {
    private final MarketDataGateway marketDataGateway;

    public DefaultConnectorProvider(MarketDataGateway marketDataGateway) {
        this.marketDataGateway = marketDataGateway;
    }

    @Override
    public MarketDataGateway marketDataGateway() {
        return marketDataGateway;
    }
}
