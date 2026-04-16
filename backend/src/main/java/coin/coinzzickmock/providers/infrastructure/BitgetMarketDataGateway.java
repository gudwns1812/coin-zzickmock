package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BitgetMarketDataGateway implements MarketDataGateway {
    private final RestClient bitgetRestClient;
    private final Map<String, MarketSnapshot> fallback = Map.of(
            "BTCUSDT", new MarketSnapshot("BTCUSDT", "Bitcoin Perpetual", 102450, 102418, 102401, 0.0001, 2.84),
            "ETHUSDT", new MarketSnapshot("ETHUSDT", "Ethereum Perpetual", 3280, 3276, 3274, 0.00008, 1.72)
    );

    public BitgetMarketDataGateway(RestClient bitgetRestClient) {
        this.bitgetRestClient = bitgetRestClient;
    }

    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        return List.of(loadMarket("BTCUSDT"), loadMarket("ETHUSDT"));
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
        try {
            BitgetTickerResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/ticker")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .build())
                    .retrieve()
                    .body(BitgetTickerResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                return fallback.get(symbol);
            }

            BitgetTickerData data = response.data().get(0);
            return new MarketSnapshot(
                    symbol,
                    "BTCUSDT".equals(symbol) ? "Bitcoin Perpetual" : "Ethereum Perpetual",
                    Double.parseDouble(data.lastPr()),
                    Double.parseDouble(data.markPrice()),
                    Double.parseDouble(data.indexPrice()),
                    Double.parseDouble(data.fundingRate()),
                    Double.parseDouble(data.change24h())
            );
        } catch (Exception ignored) {
            return fallback.get(symbol);
        }
    }

    private record BitgetTickerResponse(
            String code,
            String msg,
            List<BitgetTickerData> data
    ) {
    }

    private record BitgetTickerData(
            String symbol,
            String lastPr,
            String change24h,
            String indexPrice,
            String fundingRate,
            String markPrice
    ) {
    }
}
