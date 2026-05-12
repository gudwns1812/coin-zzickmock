package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketHistoricalCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TestConnectorProvider implements ConnectorProvider {
    private static final MarketDataGateway EMPTY_GATEWAY = new MarketDataGateway() {
        @Override
        public List<ProviderMarketSnapshot> loadSupportedMarkets() {
            return List.of();
        }

        @Override
        public ProviderMarketSnapshot loadMarket(String symbol) {
            return new ProviderMarketSnapshot(
                    symbol,
                    symbol,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        @Override
        public List<ProviderMarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return List.of();
        }

        @Override
        public List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandles(
                String symbol,
                ProviderMarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive,
                int limit
        ) {
            return List.of();
        }
    };

    private TestConnectorProvider() {
    }

    public static ConnectorProvider empty() {
        return new TestConnectorProvider();
    }

    @Override
    public MarketDataGateway marketDataGateway() {
        return EMPTY_GATEWAY;
    }
}
