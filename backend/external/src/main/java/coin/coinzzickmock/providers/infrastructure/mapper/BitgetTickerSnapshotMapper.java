package coin.coinzzickmock.providers.infrastructure.mapper;

import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import coin.coinzzickmock.providers.infrastructure.BitgetTickerData;
import coin.coinzzickmock.providers.infrastructure.BitgetTickerResponse;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BitgetTickerSnapshotMapper {
    private final Map<String, ProviderMarketSnapshot> fallbackSnapshots = Map.of(
            "BTCUSDT", new ProviderMarketSnapshot(
                    "BTCUSDT", "Bitcoin Perpetual", decimal("102450"), decimal("102418"), decimal("102401"),
                    decimal("0.0001"), decimal("2.84"), decimal("1280000000")
            ),
            "ETHUSDT", new ProviderMarketSnapshot(
                    "ETHUSDT", "Ethereum Perpetual", decimal("3280"), decimal("3276"), decimal("3274"),
                    decimal("0.00008"), decimal("1.72"), decimal("640000000")
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
                parseOptional(data.lastPr(), fallback.lastPrice()),
                parseOptional(data.markPrice(), fallback.markPrice()),
                parseOptional(data.indexPrice(), fallback.indexPrice()),
                parseOptional(data.fundingRate(), fallback.fundingRate()),
                parseOptional(data.change24h(), fallback.change24h()),
                parseOptional(data.usdtVolume(), fallback.turnover24hUsdt())
        );
    }

    public ProviderMarketSnapshot fallback(String symbol) {
        String safeSymbol = symbol == null || symbol.isBlank() ? "UNKNOWN" : symbol;
        return fallbackSnapshots.getOrDefault(safeSymbol, new ProviderMarketSnapshot(
                safeSymbol,
                displayName(safeSymbol),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
    }

    private String displayName(String symbol) {
        return switch (symbol) {
            case "BTCUSDT" -> "Bitcoin Perpetual";
            case "ETHUSDT" -> "Ethereum Perpetual";
            default -> symbol;
        };
    }

    private BigDecimal parseOptional(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
