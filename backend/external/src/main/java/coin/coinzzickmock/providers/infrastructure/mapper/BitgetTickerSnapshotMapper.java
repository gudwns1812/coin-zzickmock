package coin.coinzzickmock.providers.infrastructure.mapper;

import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import coin.coinzzickmock.providers.infrastructure.BitgetTickerData;
import coin.coinzzickmock.providers.infrastructure.BitgetTickerResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BitgetTickerSnapshotMapper {
    private final Map<String, ProviderMarketSnapshot> fallbackSnapshots = Map.of(
            "BTCUSDT", new ProviderMarketSnapshot(
                    "BTCUSDT", "Bitcoin Perpetual", 102450, 102418, 102401, 0.0001, 2.84, 1_280_000_000
            ),
            "ETHUSDT", new ProviderMarketSnapshot(
                    "ETHUSDT", "Ethereum Perpetual", 3280, 3276, 3274, 0.00008, 1.72, 640_000_000
            )
    );

    public ProviderMarketSnapshot fromResponse(String symbol, BitgetTickerResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            return fallback(symbol);
        }

        BitgetTickerData data = response.data().get(0);
        ProviderMarketSnapshot fallback = fallback(symbol);
        return new ProviderMarketSnapshot(
                symbol,
                displayName(symbol),
                Double.parseDouble(data.lastPr()),
                Double.parseDouble(data.markPrice()),
                Double.parseDouble(data.indexPrice()),
                Double.parseDouble(data.fundingRate()),
                Double.parseDouble(data.change24h()),
                parseOptional(data.usdtVolume(), fallback == null ? 0.0 : fallback.turnover24hUsdt())
        );
    }

    public ProviderMarketSnapshot fallback(String symbol) {
        return fallbackSnapshots.get(symbol);
    }

    private String displayName(String symbol) {
        return switch (symbol) {
            case "BTCUSDT" -> "Bitcoin Perpetual";
            case "ETHUSDT" -> "Ethereum Perpetual";
            default -> symbol;
        };
    }

    private double parseOptional(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(value);
    }
}
